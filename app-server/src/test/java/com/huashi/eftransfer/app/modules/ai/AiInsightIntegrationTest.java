package com.huashi.eftransfer.app.modules.ai;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.config.AiGatewayClientProperties;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayCallResult;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayClient;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayFailureReason;
import com.huashi.eftransfer.app.modules.ai.entity.AiGenerationRecordEntity;
import com.huashi.eftransfer.app.modules.ai.mapper.AiGenerationRecordMapper;
import com.huashi.eftransfer.app.modules.ai.support.AiConstants;
import com.huashi.eftransfer.app.modules.analytics.entity.InterventionRecordEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.InterventionRecordMapper;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassMapper;
import com.huashi.eftransfer.app.modules.analytics.service.AnalyticsAggregationService;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisSummaryEntity;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisSummaryMapper;
import com.huashi.eftransfer.app.modules.training.entity.TrainingItemResultEntity;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingItemResultMapper;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import com.huashi.eftransfer.shared.ai.RagAnswerRequest;
import com.huashi.eftransfer.shared.ai.RagAnswerResponse;
import com.huashi.eftransfer.shared.ai.RagCitation;
import com.huashi.eftransfer.shared.ai.RagContextChunk;
import com.huashi.eftransfer.shared.ai.RagExplainRiskRequest;
import com.huashi.eftransfer.shared.ai.RagExplainRiskResponse;
import com.huashi.eftransfer.shared.ai.RerankItem;
import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import com.huashi.eftransfer.shared.ai.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(AiInsightIntegrationTest.AiIntegrationTestConfiguration.class)
class AiInsightIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private StubAiGatewayClient stubAiGatewayClient;

    @Autowired
    private AiGenerationRecordMapper aiGenerationRecordMapper;

    @Autowired
    private InterventionRecordMapper interventionRecordMapper;

    @Autowired
    private TeachingClassMapper teachingClassMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiagnosisSummaryMapper diagnosisSummaryMapper;

    @Autowired
    private TrainingItemResultMapper trainingItemResultMapper;

    @Autowired
    private AnalyticsAggregationService analyticsAggregationService;

    @BeforeEach
    void resetStubModes() {
        stubAiGatewayClient.reset();
    }

    @Test
    void shouldGenerateAiGuidancePersistAuditRecordsAndCreateTeacherDraft() throws Exception {
        AiScenario scenario = prepareAiScenario(3, true);

        MvcResult recommendResult = mockMvc.perform(post("/api/ai/recommend-training")
                        .with(bearer(scenario.studentToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "diagnosisSummaryId": %d
                                }
                                """.formatted(scenario.latestDiagnosisSummaryId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generationSource").value(AiConstants.GENERATION_SOURCE_AI))
                .andExpect(jsonPath("$.data.promptVersion").value(AiConstants.DEFAULT_PROMPT_VERSION))
                .andExpect(jsonPath("$.data.recommendationPath.length()").value(3))
                .andExpect(jsonPath("$.data.focusLexicalPairs.length()").value(2))
                .andExpect(jsonPath("$.data.recommendedTrainingModes.length()").value(2))
                .andExpect(jsonPath("$.data.explanation").isString())
                .andExpect(jsonPath("$.data.teacherNote").isString())
                .andExpect(jsonPath("$.data.confidence").isNumber())
                .andReturn();

        MvcResult explainResult = mockMvc.perform(post("/api/ai/explain-diagnosis")
                        .with(bearer(scenario.studentToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "diagnosisSummaryId": %d
                                }
                                """.formatted(scenario.latestDiagnosisSummaryId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generationSource").value(AiConstants.GENERATION_SOURCE_AI))
                .andExpect(jsonPath("$.data.promptVersion").value(AiConstants.DEFAULT_PROMPT_VERSION))
                .andExpect(jsonPath("$.data.recommendationPath.length()").value(3))
                .andExpect(jsonPath("$.data.focusLexicalPairs.length()").value(2))
                .andExpect(jsonPath("$.data.explanation").isString())
                .andReturn();

        MvcResult teacherResult = mockMvc.perform(post("/api/teacher/intervention-suggest")
                        .with(bearer(scenario.teacherToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "classId": %d,
                                  "studentUserId": %d,
                                  "diagnosisSummaryId": %d
                                }
                                """.formatted(scenario.classId(), scenario.studentUserId(), scenario.latestDiagnosisSummaryId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generationSource").value(AiConstants.GENERATION_SOURCE_AI))
                .andExpect(jsonPath("$.data.promptVersion").value(AiConstants.DEFAULT_PROMPT_VERSION))
                .andExpect(jsonPath("$.data.recommendationPath.length()").value(3))
                .andExpect(jsonPath("$.data.focusLexicalPairs.length()").value(2))
                .andExpect(jsonPath("$.data.recommendedTrainingModes.length()").value(2))
                .andExpect(jsonPath("$.data.teacherNote").isString())
                .andReturn();

        JsonNode recommendJson = readJson(recommendResult);
        JsonNode explainJson = readJson(explainResult);
        JsonNode teacherJson = readJson(teacherResult);

        AiGenerationRecordEntity recommendRecord = generationRecord(recommendJson.path("data").path("requestId").asText());
        AiGenerationRecordEntity explainRecord = generationRecord(explainJson.path("data").path("requestId").asText());
        AiGenerationRecordEntity teacherRecord = generationRecord(teacherJson.path("data").path("requestId").asText());

        assertThat(recommendRecord.getScene()).isEqualTo(AiConstants.SCENE_RECOMMEND_TRAINING);
        assertThat(recommendRecord.getGenerationSource()).isEqualTo(AiConstants.GENERATION_SOURCE_AI);
        assertThat(recommendRecord.getPromptVersion()).isEqualTo(AiConstants.DEFAULT_PROMPT_VERSION);
        assertThat(recommendRecord.getModel()).isEqualTo("stub-structured-model");
        assertThat(recommendRecord.getFallbackReason()).isNull();
        assertThat(recommendRecord.getTokenUsageJson()).contains("promptTokens");
        assertThat(recommendRecord.getValidatedOutputJson()).contains("recommendationPath");

        assertThat(explainRecord.getScene()).isEqualTo(AiConstants.SCENE_EXPLAIN_DIAGNOSIS);
        assertThat(explainRecord.getGenerationSource()).isEqualTo(AiConstants.GENERATION_SOURCE_AI);
        assertThat(explainRecord.getFallbackReason()).isNull();

        assertThat(teacherRecord.getScene()).isEqualTo(AiConstants.SCENE_TEACHER_INTERVENTION);
        assertThat(teacherRecord.getTeacherUserId()).isEqualTo(scenario.teacherUserId());
        assertThat(teacherRecord.getTeachingClassId()).isEqualTo(scenario.classId());
        assertThat(teacherRecord.getGenerationSource()).isEqualTo(AiConstants.GENERATION_SOURCE_AI);
        assertThat(teacherRecord.getInterventionRecordId()).isNotNull();
        assertThat(teacherRecord.getFallbackReason()).isNull();

        Long sceneCount = aiGenerationRecordMapper.selectCount(Wrappers.<AiGenerationRecordEntity>lambdaQuery()
                .in(AiGenerationRecordEntity::getScene,
                        AiConstants.SCENE_RECOMMEND_TRAINING,
                        AiConstants.SCENE_EXPLAIN_DIAGNOSIS,
                        AiConstants.SCENE_TEACHER_INTERVENTION));
        assertThat(sceneCount).isEqualTo(3);

        InterventionRecordEntity interventionDraft = interventionRecordMapper.selectOne(Wrappers.<InterventionRecordEntity>lambdaQuery()
                .eq(InterventionRecordEntity::getTeacherUserId, scenario.teacherUserId())
                .eq(InterventionRecordEntity::getTeachingClassId, scenario.classId())
                .eq(InterventionRecordEntity::getStudentUserId, scenario.studentUserId())
                .eq(InterventionRecordEntity::getInterventionType, "AI_SUGGESTED")
                .eq(InterventionRecordEntity::getStatus, "PENDING")
                .orderByDesc(InterventionRecordEntity::getId)
                .last("LIMIT 1"));
        assertThat(interventionDraft).isNotNull();
        assertThat(interventionDraft.getTriggerSource()).isEqualTo("AI_TEACHER_INTERVENTION");
        assertThat(interventionDraft.getNote()).contains("教师");
        assertThat(interventionDraft.getTriggerSnapshotJson()).contains(teacherJson.path("data").path("requestId").asText());
    }

    @Test
    void shouldFallbackWhenAiReturnsInvalidStructuredPayload() throws Exception {
        AiScenario scenario = prepareAiScenario(1, true);
        stubAiGatewayClient.setStructuredMode(StubAiGatewayClient.StructuredMode.INVALID_JSON);

        MvcResult result = mockMvc.perform(post("/api/ai/recommend-training")
                        .with(bearer(scenario.studentToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "diagnosisSummaryId": %d
                                }
                                """.formatted(scenario.latestDiagnosisSummaryId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generationSource").value(AiConstants.GENERATION_SOURCE_RULE_FALLBACK))
                .andExpect(jsonPath("$.data.fallbackReason").value(AiGatewayFailureReason.INVALID_JSON.name()))
                .andExpect(jsonPath("$.data.recommendationPath.length()").value(3))
                .andExpect(jsonPath("$.data.explanation").isString())
                .andReturn();

        JsonNode json = readJson(result);
        AiGenerationRecordEntity generationRecord = generationRecord(json.path("data").path("requestId").asText());
        assertThat(generationRecord.getScene()).isEqualTo(AiConstants.SCENE_RECOMMEND_TRAINING);
        assertThat(generationRecord.getGenerationSource()).isEqualTo(AiConstants.GENERATION_SOURCE_RULE_FALLBACK);
        assertThat(generationRecord.getFallbackReason()).isEqualTo(AiGatewayFailureReason.INVALID_JSON.name());
        assertThat(generationRecord.getValidatedOutputJson()).contains("recommendationPath");
    }

    @Test
    void shouldFallbackWhenProviderIsUnavailableAndStillPersistTeacherDraft() throws Exception {
        AiScenario scenario = prepareAiScenario(3, true);
        stubAiGatewayClient.setStructuredMode(StubAiGatewayClient.StructuredMode.PROVIDER_UNAVAILABLE);

        MvcResult result = mockMvc.perform(post("/api/teacher/intervention-suggest")
                        .with(bearer(scenario.teacherToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "classId": %d,
                                  "studentUserId": %d,
                                  "diagnosisSummaryId": %d
                                }
                                """.formatted(scenario.classId(), scenario.studentUserId(), scenario.latestDiagnosisSummaryId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generationSource").value(AiConstants.GENERATION_SOURCE_RULE_FALLBACK))
                .andExpect(jsonPath("$.data.fallbackReason").value(AiGatewayFailureReason.PROVIDER_UNAVAILABLE.name()))
                .andExpect(jsonPath("$.data.teacherNote").isString())
                .andReturn();

        JsonNode json = readJson(result);
        AiGenerationRecordEntity generationRecord = generationRecord(json.path("data").path("requestId").asText());
        assertThat(generationRecord.getScene()).isEqualTo(AiConstants.SCENE_TEACHER_INTERVENTION);
        assertThat(generationRecord.getGenerationSource()).isEqualTo(AiConstants.GENERATION_SOURCE_RULE_FALLBACK);
        assertThat(generationRecord.getFallbackReason()).isEqualTo(AiGatewayFailureReason.PROVIDER_UNAVAILABLE.name());
        assertThat(generationRecord.getInterventionRecordId()).isNotNull();

        InterventionRecordEntity interventionDraft = interventionRecordMapper.selectById(generationRecord.getInterventionRecordId());
        assertThat(interventionDraft).isNotNull();
        assertThat(interventionDraft.getStatus()).isEqualTo("PENDING");
        assertThat(interventionDraft.getInterventionType()).isEqualTo("AI_SUGGESTED");
        assertThat(interventionDraft.getTriggerSource()).isEqualTo("AI_TEACHER_INTERVENTION");
        assertThat(interventionDraft.getNote()).isEqualTo(json.path("data").path("teacherNote").asText());
    }

    private AiScenario prepareAiScenario(int diagnosisRuns, boolean completeTraining) throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");

        long tablePairId = createLexicalPair(teacherToken, """
                {
                  "englishWord": "table",
                  "frenchWord": "table",
                  "chineseGloss": "桌子",
                  "lexicalPairType": "cognate",
                  "semanticOverlapScore": 0.95,
                  "falseFriendRisk": 0.05,
                  "defaultContextSupport": "low",
                  "difficultyLevel": 1,
                  "active": true,
                  "knowledgeStatus": "ready",
                  "embeddingStatus": "pending",
                  "tags": ["basic"],
                  "senses": [
                    {
                      "sortOrder": 1,
                      "englishDefinition": "a piece of furniture",
                      "frenchDefinition": "meuble avec une surface plane",
                      "chineseDefinition": "桌子",
                      "examples": [
                        {
                          "sortOrder": 1,
                          "englishExample": "The books are on the table.",
                          "frenchExample": "Les livres sont sur la table.",
                          "chineseTranslation": "书在桌子上。",
                          "contextSupportLevel": "medium",
                          "source": "Teacher Curated"
                        }
                      ]
                    }
                  ]
                }
                """);
        long coinPairId = createLexicalPair(teacherToken, """
                {
                  "englishWord": "coin",
                  "frenchWord": "coin",
                  "chineseGloss": "硬币；角落",
                  "lexicalPairType": "false_friend",
                  "semanticOverlapScore": 0.10,
                  "falseFriendRisk": 0.92,
                  "defaultContextSupport": "high",
                  "difficultyLevel": 4,
                  "active": true,
                  "knowledgeStatus": "ready",
                  "embeddingStatus": "pending",
                  "tags": ["false-friend"],
                  "senses": [
                    {
                      "sortOrder": 1,
                      "englishDefinition": "money",
                      "frenchDefinition": "corner",
                      "chineseDefinition": "角落",
                      "examples": [
                        {
                          "sortOrder": 1,
                          "englishExample": "I found a coin on the floor.",
                          "frenchExample": "Il attend au coin de la rue.",
                          "chineseTranslation": "他在街角等。",
                          "contextSupportLevel": "high",
                          "source": "Teacher Curated"
                        }
                      ]
                    }
                  ]
                }
                """);
        long actuallyPairId = createLexicalPair(teacherToken, """
                {
                  "englishWord": "actually",
                  "frenchWord": "actuellement",
                  "chineseGloss": "实际上；目前",
                  "lexicalPairType": "false_friend",
                  "semanticOverlapScore": 0.18,
                  "falseFriendRisk": 0.88,
                  "defaultContextSupport": "high",
                  "difficultyLevel": 4,
                  "active": true,
                  "knowledgeStatus": "ready",
                  "embeddingStatus": "pending",
                  "tags": ["false-friend", "context"],
                  "senses": [
                    {
                      "sortOrder": 1,
                      "englishDefinition": "currently",
                      "frenchDefinition": "currently",
                      "chineseDefinition": "目前",
                      "examples": [
                        {
                          "sortOrder": 1,
                          "englishExample": "Actually, I do not agree.",
                          "frenchExample": "Actuellement, il habite a Paris.",
                          "chineseTranslation": "目前，他住在巴黎。",
                          "contextSupportLevel": "high",
                          "source": "Teacher Curated"
                        }
                      ]
                    }
                  ]
                }
                """);

        long templateId = createDiagnosisTemplate(teacherToken, tablePairId, coinPairId, actuallyPairId);
        for (int index = 0; index < diagnosisRuns; index++) {
            createDiagnosisSessionAndComplete(studentToken, templateId);
        }
        if (completeTraining) {
            startTrainingSessionAndComplete(studentToken);
        }
        analyticsAggregationService.rebuildRange(LocalDate.now().minusDays(1), LocalDate.now());

        UserEntity student = userMapper.selectByUsernameOrEmail("student.li");
        assertThat(student).isNotNull();
        TeachingClassEntity teachingClass = teachingClassMapper.selectOne(Wrappers.<TeachingClassEntity>lambdaQuery()
                .eq(TeachingClassEntity::getClassCode, "CLS-0001")
                .last("LIMIT 1"));
        DiagnosisSummaryEntity latestDiagnosis = diagnosisSummaryMapper.selectOne(Wrappers.<DiagnosisSummaryEntity>lambdaQuery()
                .eq(DiagnosisSummaryEntity::getOwnerUserId, student.getId())
                .orderByDesc(DiagnosisSummaryEntity::getGeneratedAt)
                .orderByDesc(DiagnosisSummaryEntity::getId)
                .last("LIMIT 1"));

        assertThat(teachingClass).isNotNull();
        assertThat(latestDiagnosis).isNotNull();

        return new AiScenario(
                student.getId(),
                teachingClass.getId(),
                teachingClass.getTeacherUserId(),
                latestDiagnosis.getId(),
                studentToken,
                teacherToken
        );
    }

    private AiGenerationRecordEntity generationRecord(String requestId) {
        AiGenerationRecordEntity entity = aiGenerationRecordMapper.selectOne(Wrappers.<AiGenerationRecordEntity>lambdaQuery()
                .eq(AiGenerationRecordEntity::getRequestId, requestId)
                .last("LIMIT 1"));
        assertThat(entity).isNotNull();
        return entity;
    }

    private long createLexicalPair(String token, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/lexical-pairs")
                        .with(bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").asLong();
    }

    private long createDiagnosisTemplate(String teacherToken, long tablePairId, long coinPairId, long actuallyPairId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/diagnosis-templates")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "AI linked diagnosis",
                                  "description": "AI guidance source",
                                  "status": "published",
                                  "estimatedDurationMinutes": 10,
                                  "scoringVersion": "RULE_V1",
                                  "items": [
                                    {
                                      "lexicalPairId": %d,
                                      "taskType": "reaction_time_task",
                                      "blockCode": "block_1",
                                      "sortOrder": 1,
                                      "contextSupportLevel": "low",
                                      "expectedSemanticMatch": true,
                                      "stimulus": {
                                        "instruction": "Semantic match?",
                                        "contextSentence": "",
                                        "promptText": "Semantic match?"
                                      },
                                      "options": [
                                        { "key": "semantic_match", "label": "语义一致", "semanticMatch": true, "ignoreContextTrap": false },
                                        { "key": "semantic_mismatch", "label": "语义不一致", "semanticMatch": false, "ignoreContextTrap": false }
                                      ],
                                      "correctAnswerKey": "semantic_match"
                                    },
                                    {
                                      "lexicalPairId": %d,
                                      "taskType": "reaction_time_task",
                                      "blockCode": "block_2",
                                      "sortOrder": 2,
                                      "contextSupportLevel": "medium",
                                      "expectedSemanticMatch": false,
                                      "stimulus": {
                                        "instruction": "Trust your first response",
                                        "contextSentence": "He stood in the coin of the room.",
                                        "promptText": "Semantic match?"
                                      },
                                      "options": [
                                        { "key": "semantic_match", "label": "语义一致", "semanticMatch": true, "ignoreContextTrap": false },
                                        { "key": "semantic_mismatch", "label": "语义不一致", "semanticMatch": false, "ignoreContextTrap": false }
                                      ],
                                      "correctAnswerKey": "semantic_mismatch"
                                    },
                                    {
                                      "lexicalPairId": %d,
                                      "taskType": "semantic_judgement_task",
                                      "blockCode": "block_3",
                                      "sortOrder": 3,
                                      "contextSupportLevel": "high",
                                      "expectedSemanticMatch": false,
                                      "stimulus": {
                                        "instruction": "Read the sentence carefully",
                                        "contextSentence": "Actuellement, il travaille a Lyon.",
                                        "promptText": "Which option best fits the sentence?"
                                      },
                                      "options": [
                                        { "key": "actually_trap", "label": "Actually (实际上)", "semanticMatch": true, "ignoreContextTrap": true },
                                        { "key": "currently_correct", "label": "Currently (目前)", "semanticMatch": false, "ignoreContextTrap": false },
                                        { "key": "occasionally", "label": "Occasionally (偶尔)", "semanticMatch": false, "ignoreContextTrap": false }
                                      ],
                                      "correctAnswerKey": "currently_correct"
                                    }
                                  ]
                                }
                                """.formatted(tablePairId, coinPairId, actuallyPairId)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").asLong();
    }

    private void createDiagnosisSessionAndComplete(String studentToken, long templateId) throws Exception {
        MvcResult sessionCreateResult = mockMvc.perform(post("/api/diagnosis/sessions")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId": %d
                                }
                                """.formatted(templateId)))
                .andExpect(status().isOk())
                .andReturn();
        long sessionId = readJson(sessionCreateResult).path("data").path("sessionId").asLong();

        for (int index = 0; index < 3; index++) {
            MvcResult nextItemResult = mockMvc.perform(get("/api/diagnosis/sessions/{sessionId}/next-item", sessionId)
                            .with(bearer(studentToken)))
                    .andExpect(status().isOk())
                    .andReturn();
            long itemResultId = readJson(nextItemResult).path("data").path("item").path("itemResultId").asLong();
            String englishWord = readJson(nextItemResult).path("data").path("item").path("englishWord").asText();

            if ("table".equals(englishWord)) {
                submitDiagnosisAnswer(studentToken, sessionId, """
                        {
                          "itemResultId": %d,
                          "selectedSemanticMatch": true,
                          "reactionTimeMs": 620,
                          "hesitationTimeMs": 90
                        }
                        """.formatted(itemResultId));
            } else if ("coin".equals(englishWord)) {
                submitDiagnosisAnswer(studentToken, sessionId, """
                        {
                          "itemResultId": %d,
                          "selectedSemanticMatch": true,
                          "reactionTimeMs": 1320,
                          "hesitationTimeMs": 360
                        }
                        """.formatted(itemResultId));
            } else {
                submitDiagnosisAnswer(studentToken, sessionId, """
                        {
                          "itemResultId": %d,
                          "selectedAnswerKey": "actually_trap",
                          "reactionTimeMs": 1510,
                          "hesitationTimeMs": 410
                        }
                        """.formatted(itemResultId));
            }
        }

        mockMvc.perform(post("/api/diagnosis/sessions/{sessionId}/complete", sessionId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    private long startTrainingSessionAndComplete(String studentToken) throws Exception {
        MvcResult planResult = mockMvc.perform(get("/api/training/plans/recommended")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andReturn();
        long planId = readJson(planResult).path("data").path("planId").asLong();

        MvcResult sessionStart = mockMvc.perform(post("/api/training/sessions")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": %d,
                                  "mode": "FALSE_FRIEND_DISCRIM"
                                }
                                """.formatted(planId)))
                .andExpect(status().isOk())
                .andReturn();
        long sessionId = readJson(sessionStart).path("data").path("sessionId").asLong();

        List<TrainingItemResultEntity> itemResults = trainingItemResultMapper.selectList(Wrappers.<TrainingItemResultEntity>lambdaQuery()
                .eq(TrainingItemResultEntity::getSessionId, sessionId)
                .orderByAsc(TrainingItemResultEntity::getPresentationOrder));
        for (int index = 0; index < itemResults.size(); index++) {
            TrainingItemResultEntity itemResult = itemResults.get(index);
            mockMvc.perform(get("/api/training/sessions/{sessionId}/next-item", sessionId)
                            .with(bearer(studentToken)))
                    .andExpect(status().isOk());

            String answerKey = index == 0 ? firstWrongOptionKey(itemResult) : itemResult.getCorrectAnswerKey();
            int reactionTime = index == 0 ? 1490 : 860;
            int hesitationTime = index == 0 ? 360 : 120;
            mockMvc.perform(post("/api/training/sessions/{sessionId}/answers", sessionId)
                            .with(bearer(studentToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "itemResultId": %d,
                                      "selectedAnswerKey": "%s",
                                      "reactionTimeMs": %d,
                                      "hesitationTimeMs": %d
                                    }
                                    """.formatted(itemResult.getId(), answerKey, reactionTime, hesitationTime)))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/training/sessions/{sessionId}/complete", sessionId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        return sessionId;
    }

    private void submitDiagnosisAnswer(String studentToken, long sessionId, String body) throws Exception {
        mockMvc.perform(post("/api/diagnosis/sessions/{sessionId}/answers", sessionId)
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private String firstWrongOptionKey(TrainingItemResultEntity itemResult) throws Exception {
        JsonNode options = objectMapper.readTree(itemResult.getOptionsJson());
        for (JsonNode option : options) {
            String key = option.path("key").asText();
            if (!key.equals(itemResult.getCorrectAnswerKey())) {
                return key;
            }
        }
        throw new IllegalStateException("No wrong option found for training item " + itemResult.getId());
    }

    @TestConfiguration
    static class AiIntegrationTestConfiguration {

        @Bean
        @Primary
        StubAiGatewayClient stubAiGatewayClient(AiGatewayClientProperties properties) {
            return new StubAiGatewayClient(properties);
        }
    }

    static class StubAiGatewayClient extends AiGatewayClient {

        private static final String STRUCTURED_MODEL = "stub-structured-model";
        private static final String RERANK_MODEL = "stub-rerank-model";
        private StructuredMode structuredMode = StructuredMode.SUCCESS;

        StubAiGatewayClient(AiGatewayClientProperties properties) {
            super(RestClient.builder().baseUrl("http://localhost").build(), properties);
        }

        void reset() {
            this.structuredMode = StructuredMode.SUCCESS;
        }

        void setStructuredMode(StructuredMode structuredMode) {
            this.structuredMode = structuredMode;
        }

        @Override
        public AiGatewayCallResult<StructuredChatResponse> structuredChat(StructuredChatRequest request) {
            if (structuredMode == StructuredMode.PROVIDER_UNAVAILABLE) {
                return AiGatewayCallResult.failure(
                        AiGatewayFailureReason.PROVIDER_UNAVAILABLE,
                        "stub structured provider unavailable",
                        1,
                        9L,
                        "/internal/ai/chat/structured"
                );
            }
            if (structuredMode == StructuredMode.INVALID_JSON) {
                return AiGatewayCallResult.success(
                        new StructuredChatResponse(
                                "stub",
                                STRUCTURED_MODEL,
                                "{broken-json",
                                Map.of(),
                                "stop",
                                "structured-invalid",
                                new TokenUsage(118, 48, 166)
                        ),
                        1,
                        12L,
                        "/internal/ai/chat/structured"
                );
            }
            return AiGatewayCallResult.success(
                    new StructuredChatResponse(
                            "stub",
                            STRUCTURED_MODEL,
                            "structured-output",
                            successStructuredPayload(),
                            "stop",
                            "structured-success",
                            new TokenUsage(118, 48, 166)
                    ),
                    1,
                    12L,
                    "/internal/ai/chat/structured"
            );
        }

        @Override
        public AiGatewayCallResult<RerankResponse> rerank(RerankRequest request) {
            int limit = Math.min(request.topN(), request.documents().size());
            List<RerankItem> items = IntStream.range(0, limit)
                    .mapToObj(index -> new RerankItem(index, 0.95d - index * 0.05d, request.documents().get(index)))
                    .toList();
            return AiGatewayCallResult.success(
                    new RerankResponse("stub", RERANK_MODEL, "rerank-success", 14, items),
                    1,
                    4L,
                    "/internal/ai/rerank"
            );
        }

        @Override
        public AiGatewayCallResult<RagAnswerResponse> ragAnswer(RagAnswerRequest request) {
            List<RagCitation> citations = List.of(new RagCitation(
                    "C1",
                    "TRAINING_GUIDE",
                    "guide-01",
                    "Personalized Training Guide",
                    "Use contrastive examples before speed practice.",
                    0.92d
            ));
            List<RagContextChunk> chunks = List.of(new RagContextChunk(
                    "C1",
                    "TRAINING_GUIDE",
                    "guide-01",
                    "Personalized Training Guide",
                    "Use contrastive examples before speed practice for high-risk false friends.",
                    "Use contrastive examples before speed practice.",
                    0.92d,
                    Map.of("chunkKind", "TRAINING_GUIDE")
            ));
            return AiGatewayCallResult.success(
                    new RagAnswerResponse(
                            "先做高风险近形词对的对比训练，再进入限时巩固。",
                            true,
                            null,
                            citations,
                            chunks
                    ),
                    1,
                    6L,
                    "/internal/ai/rag/answer"
            );
        }

        @Override
        public AiGatewayCallResult<RagExplainRiskResponse> explainRisk(RagExplainRiskRequest request) {
            List<RagCitation> citations = List.of(new RagCitation(
                    "C2",
                    "ERROR_TYPE",
                    "false_friend_confusion",
                    "False Friend Confusion",
                    "Students over-trust surface similarity when context sensitivity is low.",
                    0.89d
            ));
            List<RagContextChunk> chunks = List.of(new RagContextChunk(
                    "C2",
                    "ERROR_TYPE",
                    "false_friend_confusion",
                    "False Friend Confusion",
                    "Students over-trust surface similarity when context sensitivity is low.",
                    "Students over-trust surface similarity when context sensitivity is low.",
                    0.89d,
                    Map.of("chunkKind", "ERROR_TYPE")
            ));
            return AiGatewayCallResult.success(
                    new RagExplainRiskResponse(
                            "学生目前会优先依赖表层词形，而不是完整语境。",
                            "高风险词对触发了典型的 false friend confusion。",
                            "优先做近形近义词对比辨析，再补语境修正。",
                            null,
                            citations,
                            chunks
                    ),
                    1,
                    5L,
                    "/internal/ai/rag/explain-risk"
            );
        }

        private Map<String, Object> successStructuredPayload() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("recommendationPath", List.of(
                    pathItem("锁定高风险词对", "先围绕 false friend 误判最集中的词对做短时对比辨析。", "HIGH"),
                    pathItem("切入专项训练", "先做错因拆解，再进入语境修正和短时巩固。", "HIGH"),
                    pathItem("安排课堂复盘", "教师在下次课堂中追问判断依据，确认学生不再只看词形。", "MEDIUM")
            ));
            payload.put("focusLexicalPairs", List.of(
                    focusPair(1001L, "coin", "coin", "硬币；角落", "FALSE_FRIEND", 0.91d, "FALSE_FRIEND_CONFUSION", "最近诊断中误判最稳定，优先级最高。"),
                    focusPair(1002L, "actually", "actuellement", "实际上；目前", "FALSE_FRIEND", 0.87d, "CONTEXT_IGNORANCE", "语境切换时仍会被英文熟词义误导。")
            ));
            payload.put("recommendedTrainingModes", List.of(
                    modeItem("FALSE_FRIEND_DISCRIM", "假朋友辨析训练", "先修正近形近义词的主导误判模式。"),
                    modeItem("CONTEXT_FIX", "语境修正训练", "在修正错因后，立刻迁移到新语境验证。")
            ));
            payload.put("explanation", "学生当前最需要先稳住高风险词对辨析，再把正确判断迁移到新语境中。");
            payload.put("teacherNote", "教师可先讲清最小语义差异，再让学生口头复述判断路径，避免只凭词形作答。");
            payload.put("confidence", 0.91d);
            return payload;
        }

        private Map<String, Object> pathItem(String title, String reason, String priority) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("title", title);
            item.put("reason", reason);
            item.put("priority", priority);
            return item;
        }

        private Map<String, Object> focusPair(
                Long lexicalPairId,
                String englishWord,
                String frenchWord,
                String chineseGloss,
                String lexicalPairType,
                double riskScore,
                String dominantErrorType,
                String focusReason
        ) {
            Map<String, Object> pair = new LinkedHashMap<>();
            pair.put("lexicalPairId", lexicalPairId);
            pair.put("englishWord", englishWord);
            pair.put("frenchWord", frenchWord);
            pair.put("chineseGloss", chineseGloss);
            pair.put("lexicalPairType", lexicalPairType);
            pair.put("riskScore", riskScore);
            pair.put("dominantErrorType", dominantErrorType);
            pair.put("focusReason", focusReason);
            return pair;
        }

        private Map<String, Object> modeItem(String mode, String label, String reason) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("mode", mode);
            item.put("label", label);
            item.put("reason", reason);
            return item;
        }

        enum StructuredMode {
            SUCCESS,
            INVALID_JSON,
            PROVIDER_UNAVAILABLE
        }
    }

    private record AiScenario(
            Long studentUserId,
            Long classId,
            Long teacherUserId,
            Long latestDiagnosisSummaryId,
            String studentToken,
            String teacherToken
    ) {
    }
}
