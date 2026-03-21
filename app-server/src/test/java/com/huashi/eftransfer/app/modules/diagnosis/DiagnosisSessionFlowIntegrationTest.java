package com.huashi.eftransfer.app.modules.diagnosis;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.audit.entity.AuditLogEntity;
import com.huashi.eftransfer.app.common.audit.mapper.AuditLogMapper;
import com.huashi.eftransfer.app.modules.diagnosis.event.DiagnosisCompletedEvent;
import com.huashi.eftransfer.app.modules.diagnosis.event.DiagnosisCompletedEventPublisher;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(DiagnosisSessionFlowIntegrationTest.DiagnosisTestConfiguration.class)
class DiagnosisSessionFlowIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private RecordingDiagnosisCompletedEventPublisher recordingDiagnosisCompletedEventPublisher;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Test
    void shouldRunDiagnosisSessionWorkflowAndPersistSummary() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");

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
                  "tags": ["basic"]
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
                  "defaultContextSupport": "medium",
                  "difficultyLevel": 4,
                  "active": true,
                  "tags": ["false-friend"]
                }
                """);
        long actuallyPairId = createLexicalPair(teacherToken, """
                {
                  "englishWord": "actually",
                  "frenchWord": "actuellement",
                  "chineseGloss": "实际上；目前",
                  "lexicalPairType": "false_friend",
                  "semanticOverlapScore": 0.20,
                  "falseFriendRisk": 0.88,
                  "defaultContextSupport": "high",
                  "difficultyLevel": 4,
                  "active": true,
                  "tags": ["false-friend", "context"]
                }
                """);

        MvcResult templateCreateResult = mockMvc.perform(post("/api/teacher/diagnosis-templates")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "Cognitive Transfer Diagnostic A",
                                  "description": "Pilot session",
                                  "status": "published",
                                  "estimatedDurationMinutes": 12,
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
                                        "instruction": "Quickly decide whether the core meaning matches",
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
                                        "instruction": "Use the sentence context to choose the correct gloss",
                                        "contextSentence": "Il travaille actuellement a Paris.",
                                        "promptText": "Which gloss fits the French word?"
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
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn();
        long templateId = readJson(templateCreateResult).path("data").asLong();

        mockMvc.perform(get("/api/teacher/diagnosis-templates/{id}", templateId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.items.length()").value(3));

        MvcResult sessionCreateResult = mockMvc.perform(post("/api/diagnosis/sessions")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId": %d
                                }
                                """.formatted(templateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.totalItems").value(3))
                .andReturn();
        long sessionId = readJson(sessionCreateResult).path("data").path("sessionId").asLong();

        mockMvc.perform(post("/api/diagnosis/sessions")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId": %d
                                }
                                """.formatted(templateId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Diagnosis session already in progress. Resume the active session before starting a new one."));

        for (int i = 0; i < 3; i++) {
            MvcResult nextItemResult = mockMvc.perform(get("/api/diagnosis/sessions/{sessionId}/next-item", sessionId)
                            .with(bearer(studentToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hasNextItem").value(true))
                    .andReturn();
            long itemResultId = readJson(nextItemResult).path("data").path("item").path("itemResultId").asLong();
            String englishWord = readJson(nextItemResult).path("data").path("item").path("englishWord").asText();

            if ("table".equals(englishWord)) {
                mockMvc.perform(post("/api/diagnosis/sessions/{sessionId}/answers", sessionId)
                                .with(bearer(studentToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "itemResultId": %d,
                                          "selectedSemanticMatch": true,
                                          "reactionTimeMs": 620,
                                          "hesitationTimeMs": 90
                                        }
                                        """.formatted(itemResultId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.answeredItems").value(1));

                mockMvc.perform(post("/api/diagnosis/sessions/{sessionId}/progress", sessionId)
                                .with(bearer(studentToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "progressSnapshot": {
                                            "checkpoint": "after_table",
                                            "clientElapsedMs": 620
                                          }
                                        }
                                        """))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.answeredItems").value(1));
            } else if ("coin".equals(englishWord)) {
                mockMvc.perform(post("/api/diagnosis/sessions/{sessionId}/answers", sessionId)
                                .with(bearer(studentToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "itemResultId": %d,
                                          "selectedSemanticMatch": true,
                                          "reactionTimeMs": 1280,
                                          "hesitationTimeMs": 340
                                        }
                                        """.formatted(itemResultId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.answeredItems").value(2));
            } else if ("actually".equals(englishWord)) {
                mockMvc.perform(post("/api/diagnosis/sessions/{sessionId}/answers", sessionId)
                                .with(bearer(studentToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "itemResultId": %d,
                                          "selectedAnswerKey": "actually_trap",
                                          "reactionTimeMs": 1510,
                                          "hesitationTimeMs": 410
                                        }
                                        """.formatted(itemResultId)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.answeredItems").value(3));
            }
        }

        mockMvc.perform(post("/api/diagnosis/sessions/{sessionId}/complete", sessionId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completed").value(true));

        mockMvc.perform(get("/api/diagnosis/sessions/{sessionId}/result", sessionId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metrics.positiveTransferScore").isNumber())
                .andExpect(jsonPath("$.data.metrics.negativeTransferRisk").isNumber())
                .andExpect(jsonPath("$.data.metrics.contextSensitivity").isNumber())
                .andExpect(jsonPath("$.data.metrics.semanticDiscrimination").isNumber())
                .andExpect(jsonPath("$.data.metrics.overallAccuracy").isNumber())
                .andExpect(jsonPath("$.data.metrics.averageReactionTime").isNumber())
                .andExpect(jsonPath("$.data.errorTypeDistribution.length()").value(6))
                .andExpect(jsonPath("$.data.highRiskLexicalPairs.length()").value(2))
                .andExpect(jsonPath("$.data.chartPayload.topRiskPairs.length()").value(2))
                .andExpect(jsonPath("$.data.items.length()").value(3));

        mockMvc.perform(get("/api/diagnosis/sessions")
                        .with(bearer(studentToken))
                        .param("pageNo", "1")
                        .param("pageSize", "10")
                        .param("status", "completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].templateId").value((int) templateId));

        mockMvc.perform(get("/api/diagnosis/sessions/{sessionId}/result", sessionId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/diagnosis/sessions/{sessionId}/result", sessionId)
                        .with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value((int) sessionId));

        assertThat(recordingDiagnosisCompletedEventPublisher.events()).hasSize(1);
        DiagnosisCompletedEvent event = recordingDiagnosisCompletedEventPublisher.events().getFirst();
        assertThat(event.sessionId()).isEqualTo(sessionId);
        assertThat(event.summaryId()).isNotNull();
        assertThat(event.highRiskLexicalPairs()).hasSize(2);

        long auditCount = auditLogMapper.selectCount(Wrappers.<AuditLogEntity>lambdaQuery()
                .in(AuditLogEntity::getActionType, List.of("template_create", "create_session", "save_progress", "submit_answer", "complete_session")));
        assertThat(auditCount).isGreaterThanOrEqualTo(6);
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

    @TestConfiguration
    static class DiagnosisTestConfiguration {

        @Bean
        @Primary
        RecordingDiagnosisCompletedEventPublisher recordingDiagnosisCompletedEventPublisher() {
            return new RecordingDiagnosisCompletedEventPublisher();
        }
    }

    static class RecordingDiagnosisCompletedEventPublisher implements DiagnosisCompletedEventPublisher {

        private final List<DiagnosisCompletedEvent> events = new ArrayList<>();

        @Override
        public void publish(DiagnosisCompletedEvent event) {
            events.add(event);
        }

        public List<DiagnosisCompletedEvent> events() {
            return events;
        }
    }
}
