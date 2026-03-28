package com.huashi.eftransfer.app.modules.analytics;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.analytics.entity.AnalyticsDailyAggregateEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.ClassAnalyticsDailyAggregateEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.LearningProfileSnapshotEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.AnalyticsDailyAggregateMapper;
import com.huashi.eftransfer.app.modules.analytics.mapper.ClassAnalyticsDailyAggregateMapper;
import com.huashi.eftransfer.app.modules.analytics.mapper.LearningProfileSnapshotMapper;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassMapper;
import com.huashi.eftransfer.app.modules.analytics.service.AnalyticsAggregationService;
import com.huashi.eftransfer.app.modules.training.entity.TrainingItemResultEntity;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingItemResultMapper;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnalyticsIntegrationTest extends AbstractWebIntegrationTest {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv");

    @Autowired
    private AnalyticsDailyAggregateMapper analyticsDailyAggregateMapper;

    @Autowired
    private ClassAnalyticsDailyAggregateMapper classAnalyticsDailyAggregateMapper;

    @Autowired
    private LearningProfileSnapshotMapper learningProfileSnapshotMapper;

    @Autowired
    private TeachingClassMapper teachingClassMapper;

    @Autowired
    private TrainingItemResultMapper trainingItemResultMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AnalyticsAggregationService analyticsAggregationService;

    @Test
    void shouldAggregateStudentAndClassAnalyticsAfterDiagnosisAndTraining() throws Exception {
        AnalyticsScenario scenario = prepareAnalyticsScenario();

        Long studentAggregateCount = analyticsDailyAggregateMapper.selectCount(Wrappers.<AnalyticsDailyAggregateEntity>lambdaQuery());
        Long classAggregateCount = classAnalyticsDailyAggregateMapper.selectCount(Wrappers.<ClassAnalyticsDailyAggregateEntity>lambdaQuery()
                .eq(ClassAnalyticsDailyAggregateEntity::getTeachingClassId, scenario.classId()));
        LearningProfileSnapshotEntity studentSnapshot = learningProfileSnapshotMapper.selectOne(Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                .eq(LearningProfileSnapshotEntity::getScope, "STUDENT")
                .eq(LearningProfileSnapshotEntity::getStudentUserId, scenario.studentUserId())
                .last("LIMIT 1"));
        LearningProfileSnapshotEntity classSnapshot = learningProfileSnapshotMapper.selectOne(Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                .eq(LearningProfileSnapshotEntity::getScope, "CLASS")
                .eq(LearningProfileSnapshotEntity::getTeachingClassId, scenario.classId())
                .last("LIMIT 1"));

        assertThat(studentAggregateCount).isNotNull().isGreaterThan(0);
        assertThat(classAggregateCount).isNotNull().isGreaterThan(0);
        assertThat(studentSnapshot).isNotNull();
        assertThat(studentSnapshot.getSnapshotJson()).contains("studentName");
        assertThat(studentSnapshot.getRecentAccuracy()).isNotNull();
        assertThat(classSnapshot).isNotNull();
        assertThat(classSnapshot.getSnapshotJson()).contains("className");
        assertThat(classSnapshot.getRecentNegativeTransferRisk()).isNotNull();

        analyticsAggregationService.rebuildRange(LocalDate.now().minusDays(1), LocalDate.now());
        analyticsAggregationService.rebuildRange(LocalDate.now().minusDays(1), LocalDate.now());
        Long rebuiltStudentAggregateCount = analyticsDailyAggregateMapper.selectCount(Wrappers.<AnalyticsDailyAggregateEntity>lambdaQuery());
        Long rebuiltClassAggregateCount = classAnalyticsDailyAggregateMapper.selectCount(Wrappers.<ClassAnalyticsDailyAggregateEntity>lambdaQuery()
                .eq(ClassAnalyticsDailyAggregateEntity::getTeachingClassId, scenario.classId()));
        assertThat(rebuiltStudentAggregateCount).isEqualTo(studentAggregateCount);
        assertThat(rebuiltClassAggregateCount).isEqualTo(classAggregateCount);
    }

    @Test
    void shouldExposeStudentTeacherAnalyticsApisAndCsvExports() throws Exception {
        AnalyticsScenario scenario = prepareAnalyticsScenario();
        String studentToken = scenario.studentToken();
        String teacherToken = scenario.teacherToken();

        mockMvc.perform(get("/api/student/analytics/overview")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentUserId").value(scenario.studentUserId()))
                .andExpect(jsonPath("$.data.cards.length()").value(4))
                .andExpect(jsonPath("$.data.contextPerformance.length()").value(3));

        mockMvc.perform(get("/api/student/analytics/trends")
                        .param("range", "7d")
                        .param("bucket", "day")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bucket").value("day"))
                .andExpect(jsonPath("$.data.series.length()").value(4));

        mockMvc.perform(get("/api/student/analytics/transfer-heatmap")
                        .param("range", "30d")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meta.range").value("30d"))
                .andExpect(jsonPath("$.data.cells.length()").value(24));

        mockMvc.perform(get("/api/student/analytics/scatter")
                        .param("range", "30d")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.x").value("avgReactionTimeMs"))
                .andExpect(jsonPath("$.data.y").value("accuracy"))
                .andExpect(jsonPath("$.data.points[0].lexicalPairId").isNumber());

        mockMvc.perform(get("/api/student/analytics/high-risk-pairs")
                        .param("range", "30d")
                        .param("limit", "5")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].lexicalPairId").isNumber());

        mockMvc.perform(get("/api/student/analytics/error-distribution")
                        .param("range", "30d")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6));

        MvcResult studentExportResult = mockMvc.perform(get("/api/student/analytics/export.csv")
                        .param("range", "30d")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TEXT_CSV))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("TOP_RISK")))
                .andReturn();
        assertThat(studentExportResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("student-analytics");

        mockMvc.perform(get("/api/teacher/analytics/classes")
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].classId").value(scenario.classId()));

        mockMvc.perform(get("/api/teacher/analytics/classes/{classId}/overview", scenario.classId())
                        .param("range", "30d")
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.classId").value(scenario.classId()))
                .andExpect(jsonPath("$.data.cards.length()").value(4));

        mockMvc.perform(get("/api/teacher/analytics/classes/{classId}/risk-distribution", scenario.classId())
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5));

        mockMvc.perform(get("/api/teacher/analytics/classes/{classId}/transfer-heatmap", scenario.classId())
                        .param("range", "30d")
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meta.range").value("30d"))
                .andExpect(jsonPath("$.data.cells.length()").value(24));

        mockMvc.perform(get("/api/teacher/analytics/classes/{classId}/error-distribution", scenario.classId())
                        .param("range", "30d")
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(6));

        mockMvc.perform(get("/api/teacher/analytics/classes/{classId}/completion-rate", scenario.classId())
                        .param("range", "30d")
                        .param("bucket", "day")
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallRate").isNumber())
                .andExpect(jsonPath("$.data.byMode[0].completedStudentCount").isNumber())
                .andExpect(jsonPath("$.data.byMode[0].label").doesNotExist())
                .andExpect(jsonPath("$.data.trend.series.length()").value(2));

        mockMvc.perform(get("/api/teacher/analytics/classes/{classId}/students", scenario.classId())
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].studentUserId").isNumber());

        mockMvc.perform(get("/api/teacher/analytics/classes/{classId}/students/{studentUserId}", scenario.classId(), scenario.studentUserId())
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.studentUserId").value(scenario.studentUserId()))
                .andExpect(jsonPath("$.data.analysis.overview.studentUserId").value(scenario.studentUserId()))
                .andExpect(jsonPath("$.data.analysis.trend30d.series.length()").value(4));

        MvcResult classExportResult = mockMvc.perform(get("/api/teacher/analytics/classes/{classId}/export.csv", scenario.classId())
                        .param("range", "30d")
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(TEXT_CSV))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("STUDENT_SUMMARY")))
                .andReturn();
        assertThat(classExportResult.getResponse().getHeader(HttpHeaders.CONTENT_DISPOSITION)).contains("class-analytics");

        mockMvc.perform(get("/api/teacher/analytics/classes/{classId}/overview", scenario.classId())
                        .with(bearer(studentToken)))
                .andExpect(status().isForbidden());
    }

    private AnalyticsScenario prepareAnalyticsScenario() throws Exception {
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
        createDiagnosisSessionAndComplete(studentToken, templateId);
        long trainingSessionId = startTrainingSessionAndComplete(studentToken);

        UserEntity student = userMapper.selectByUsernameOrEmail("student.li");
        TeachingClassEntity teachingClass = teachingClassMapper.selectOne(Wrappers.<TeachingClassEntity>lambdaQuery()
                .eq(TeachingClassEntity::getClassCode, "CLS-0001")
                .last("LIMIT 1"));
        assertThat(student).isNotNull();
        assertThat(teachingClass).isNotNull();
        assertThat(trainingSessionId).isPositive();
        return new AnalyticsScenario(student.getId(), teachingClass.getId(), studentToken, teacherToken);
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
                                  "templateName": "Analytics linked diagnosis",
                                  "description": "Analytics source",
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

        for (int i = 0; i < 3; i++) {
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

    private record AnalyticsScenario(
            Long studentUserId,
            Long classId,
            String studentToken,
            String teacherToken
    ) {
    }
}
