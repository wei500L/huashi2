package com.huashi.eftransfer.app.modules.training;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.diagnosis.event.DiagnosisCompletedEvent;
import com.huashi.eftransfer.app.modules.diagnosis.event.DiagnosisCompletedEventPublisher;
import com.huashi.eftransfer.app.modules.training.entity.ReviewScheduleEntity;
import com.huashi.eftransfer.app.modules.training.entity.TrainingItemResultEntity;
import com.huashi.eftransfer.app.modules.training.entity.WrongBookEntity;
import com.huashi.eftransfer.app.modules.training.mapper.ReviewScheduleMapper;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingItemResultMapper;
import com.huashi.eftransfer.app.modules.training.mapper.WrongBookMapper;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TrainingReviewScheduleIntegrationTest.TrainingReviewTestConfiguration.class)
class TrainingReviewScheduleIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private TrainingItemResultMapper trainingItemResultMapper;

    @Autowired
    private WrongBookMapper wrongBookMapper;

    @Autowired
    private ReviewScheduleMapper reviewScheduleMapper;

    @Test
    void shouldRefreshReviewScheduleInsteadOfDuplicatingPendingEntries() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");

        long coinPairId = createLexicalPair(teacherToken);
        long templateId = createDiagnosisTemplate(teacherToken, coinPairId);
        createDiagnosisSessionAndComplete(studentToken, templateId);

        long planId = readJson(mockMvc.perform(get("/api/training/plans/recommended")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andReturn()).path("data").path("planId").asLong();

        long firstSessionId = startTrainingSession(studentToken, planId);
        answerAllWrong(studentToken, firstSessionId);
        completeTrainingSession(studentToken, firstSessionId);

        WrongBookEntity firstWrongBook = wrongBookMapper.selectOne(Wrappers.<WrongBookEntity>lambdaQuery()
                .eq(WrongBookEntity::getLexicalPairId, coinPairId));
        List<ReviewScheduleEntity> firstSchedules = reviewScheduleMapper.selectList(Wrappers.<ReviewScheduleEntity>lambdaQuery()
                .eq(ReviewScheduleEntity::getWrongBookId, firstWrongBook.getId())
                .orderByAsc(ReviewScheduleEntity::getScheduleStage));
        assertThat(firstWrongBook.getWrongCount()).isEqualTo(1);
        assertThat(firstSchedules).hasSize(4);
        LocalDateTime firstDueAt = firstSchedules.getFirst().getDueAt();

        long secondSessionId = startTrainingSession(studentToken, planId);
        answerAllWrong(studentToken, secondSessionId);
        completeTrainingSession(studentToken, secondSessionId);

        WrongBookEntity refreshedWrongBook = wrongBookMapper.selectOne(Wrappers.<WrongBookEntity>lambdaQuery()
                .eq(WrongBookEntity::getLexicalPairId, coinPairId));
        List<ReviewScheduleEntity> refreshedSchedules = reviewScheduleMapper.selectList(Wrappers.<ReviewScheduleEntity>lambdaQuery()
                .eq(ReviewScheduleEntity::getWrongBookId, refreshedWrongBook.getId())
                .orderByAsc(ReviewScheduleEntity::getScheduleStage));

        assertThat(refreshedWrongBook.getWrongCount()).isEqualTo(2);
        assertThat(refreshedSchedules).hasSize(4);
        assertThat(refreshedSchedules).allMatch(schedule -> "PENDING".equals(schedule.getStatus()));
        assertThat(refreshedSchedules.getFirst().getDueAt()).isAfterOrEqualTo(firstDueAt);
    }

    private long createLexicalPair(String teacherToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/lexical-pairs")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "englishWord": "coin",
                                  "frenchWord": "coin",
                                  "chineseGloss": "硬币；角落",
                                  "lexicalPairType": "false_friend",
                                  "semanticOverlapScore": 0.08,
                                  "falseFriendRisk": 0.94,
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
                                          "englishExample": "I found a coin.",
                                          "frenchExample": "Elle attend au coin du cafe.",
                                          "chineseTranslation": "她在咖啡馆拐角等。",
                                          "contextSupportLevel": "high",
                                          "source": "Teacher Curated"
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").asLong();
    }

    private long createDiagnosisTemplate(String teacherToken, long coinPairId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/diagnosis-templates")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "False friend diagnosis",
                                  "description": "Single-pair training source",
                                  "status": "published",
                                  "estimatedDurationMinutes": 5,
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
                                        "instruction": "Trust your first response",
                                        "contextSentence": "He found a coin on the floor.",
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
                                        "contextSentence": "He waited in the coin of the room.",
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
                                        "contextSentence": "Elle attend au coin du cafe.",
                                        "promptText": "Which option best fits the sentence?"
                                      },
                                      "options": [
                                        { "key": "coin_money", "label": "Coin (硬币)", "semanticMatch": true, "ignoreContextTrap": true },
                                        { "key": "coin_corner", "label": "Corner (角落)", "semanticMatch": false, "ignoreContextTrap": false },
                                        { "key": "coin_song", "label": "Song (歌曲)", "semanticMatch": false, "ignoreContextTrap": false }
                                      ],
                                      "correctAnswerKey": "coin_corner"
                                    }
                                  ]
                                }
                                """.formatted(coinPairId, coinPairId, coinPairId)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").asLong();
    }

    private void createDiagnosisSessionAndComplete(String studentToken, long templateId) throws Exception {
        long sessionId = readJson(mockMvc.perform(post("/api/diagnosis/sessions")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId": %d
                                }
                                """.formatted(templateId)))
                .andExpect(status().isOk())
                .andReturn()).path("data").path("sessionId").asLong();

        for (int itemIndex = 0; itemIndex < 3; itemIndex++) {
            long itemResultId = readJson(mockMvc.perform(get("/api/diagnosis/sessions/{sessionId}/next-item", sessionId)
                            .with(bearer(studentToken)))
                    .andExpect(status().isOk())
                    .andReturn()).path("data").path("item").path("itemResultId").asLong();

            String answerPayload = switch (itemIndex) {
                case 0 -> """
                        {
                          "itemResultId": %d,
                          "selectedSemanticMatch": false,
                          "reactionTimeMs": 1380,
                          "hesitationTimeMs": 320
                        }
                        """.formatted(itemResultId);
                case 1 -> """
                        {
                          "itemResultId": %d,
                          "selectedSemanticMatch": true,
                          "reactionTimeMs": 1450,
                          "hesitationTimeMs": 360
                        }
                        """.formatted(itemResultId);
                default -> """
                        {
                          "itemResultId": %d,
                          "selectedAnswerKey": "coin_money",
                          "reactionTimeMs": 1510,
                          "hesitationTimeMs": 410
                        }
                        """.formatted(itemResultId);
            };

            mockMvc.perform(post("/api/diagnosis/sessions/{sessionId}/answers", sessionId)
                            .with(bearer(studentToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(answerPayload))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/diagnosis/sessions/{sessionId}/complete", sessionId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk());
    }

    private long startTrainingSession(String studentToken, long planId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/training/sessions")
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
        return readJson(result).path("data").path("sessionId").asLong();
    }

    private void answerAllWrong(String studentToken, long sessionId) throws Exception {
        List<TrainingItemResultEntity> itemResults = trainingItemResultMapper.selectList(Wrappers.<TrainingItemResultEntity>lambdaQuery()
                .eq(TrainingItemResultEntity::getSessionId, sessionId)
                .orderByAsc(TrainingItemResultEntity::getPresentationOrder));
        for (TrainingItemResultEntity itemResult : itemResults) {
            String wrongKey = firstWrongOptionKey(itemResult);
            mockMvc.perform(post("/api/training/sessions/{sessionId}/answers", sessionId)
                            .with(bearer(studentToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "itemResultId": %d,
                                      "selectedAnswerKey": "%s",
                                      "reactionTimeMs": 1380,
                                      "hesitationTimeMs": 320
                                    }
                                    """.formatted(itemResult.getId(), wrongKey)))
                    .andExpect(status().isOk());
        }
    }

    private void completeTrainingSession(String studentToken, long sessionId) throws Exception {
        mockMvc.perform(post("/api/training/sessions/{sessionId}/complete", sessionId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk());
    }

    private String firstWrongOptionKey(TrainingItemResultEntity itemResult) throws Exception {
        for (var option : objectMapper.readTree(itemResult.getOptionsJson())) {
            String key = option.path("key").asText();
            if (!key.equals(itemResult.getCorrectAnswerKey())) {
                return key;
            }
        }
        throw new IllegalStateException("No wrong option found for item " + itemResult.getId());
    }

    @TestConfiguration
    static class TrainingReviewTestConfiguration {

        @Bean
        @Primary
        SilentDiagnosisCompletedEventPublisher silentDiagnosisCompletedEventPublisher() {
            return new SilentDiagnosisCompletedEventPublisher();
        }
    }

    static class SilentDiagnosisCompletedEventPublisher implements DiagnosisCompletedEventPublisher {

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
