package com.huashi.eftransfer.app.modules.training;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.achievement.entity.AchievementEntity;
import com.huashi.eftransfer.app.modules.achievement.mapper.AchievementMapper;
import com.huashi.eftransfer.app.modules.analytics.entity.LearningProfileSnapshotEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.LearningProfileSnapshotMapper;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisSummaryEntity;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisSummaryMapper;
import com.huashi.eftransfer.app.modules.notification.entity.NotificationEntity;
import com.huashi.eftransfer.app.modules.notification.mapper.NotificationMapper;
import com.huashi.eftransfer.app.modules.training.entity.TrainingItemResultEntity;
import com.huashi.eftransfer.app.modules.training.entity.TrainingSessionEntity;
import com.huashi.eftransfer.app.modules.training.mapper.ReviewScheduleMapper;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingItemResultMapper;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingSessionMapper;
import com.huashi.eftransfer.app.modules.training.mapper.WrongBookMapper;
import com.huashi.eftransfer.app.modules.training.service.TrainingSessionService;
import com.huashi.eftransfer.app.modules.user.entity.StudentProfileEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrainingSessionFlowIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private TrainingItemResultMapper trainingItemResultMapper;

    @Autowired
    private TrainingSessionMapper trainingSessionMapper;

    @Autowired
    private TrainingSessionService trainingSessionService;

    @Autowired
    private WrongBookMapper wrongBookMapper;

    @Autowired
    private ReviewScheduleMapper reviewScheduleMapper;

    @Autowired
    private StudentProfileMapper studentProfileMapper;

    @Autowired
    private DiagnosisSummaryMapper diagnosisSummaryMapper;

    @Autowired
    private LearningProfileSnapshotMapper learningProfileSnapshotMapper;

    @Autowired
    private AchievementMapper achievementMapper;

    @Autowired
    private NotificationMapper notificationMapper;

    @Test
    void shouldGenerateRecommendedPlanRunTrainingAndPersistSummaryArtifacts() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");
        String otherStudentToken = loginAndGetAccessToken("student.wang", "Student@123456");

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
        long diagnosisSessionId = createDiagnosisSessionAndComplete(studentToken, templateId);
        DiagnosisSummaryEntity diagnosisSummary = diagnosisSummaryMapper.selectOne(Wrappers.<DiagnosisSummaryEntity>lambdaQuery()
                .eq(DiagnosisSummaryEntity::getSessionId, diagnosisSessionId)
                .last("LIMIT 1"));
        assertThat(diagnosisSummary).isNotNull();
        assertThat(learningProfileSnapshotMapper.selectCount(Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                .eq(LearningProfileSnapshotEntity::getLastDiagnosisSummaryId, diagnosisSummary.getId()))).isGreaterThanOrEqualTo(1);
        AchievementEntity diagnosisFinisher = achievementMapper.selectOne(Wrappers.<AchievementEntity>lambdaQuery()
                .eq(AchievementEntity::getOwnerUserId, diagnosisSummary.getOwnerUserId())
                .eq(AchievementEntity::getAchievementCode, "DIAGNOSIS_FINISHER")
                .last("LIMIT 1"));
        assertThat(diagnosisFinisher).isNotNull();
        assertThat(diagnosisFinisher.getUnlocked()).isTrue();
        assertThat(notificationMapper.selectCount(Wrappers.<NotificationEntity>lambdaQuery()
                .eq(NotificationEntity::getCategory, "DIAGNOSIS_COMPLETED")
                .like(NotificationEntity::getPayloadJson, "\"summaryId\":" + diagnosisSummary.getId()))).isGreaterThanOrEqualTo(1);

        MvcResult planResult = mockMvc.perform(get("/api/training/plans/recommended")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sourceDiagnosisSessionId").value((int) diagnosisSessionId))
                .andExpect(jsonPath("$.data.priorityMode").value("FALSE_FRIEND_DISCRIM"))
                .andExpect(jsonPath("$.data.recommendedPairs.length()").value(3))
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
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andReturn();
        long trainingSessionId = readJson(sessionStart).path("data").path("sessionId").asLong();

        mockMvc.perform(get("/api/training/sessions")
                        .with(bearer(studentToken))
                        .param("status", "IN_PROGRESS")
                        .param("pageNo", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].sessionId").value((int) trainingSessionId))
                .andExpect(jsonPath("$.data.records[0].status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/training/sessions/{sessionId}/progress", trainingSessionId)
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "progressSnapshot": {
                                    "sessionId": %d,
                                    "currentItemOrder": 1,
                                    "answeredItems": 0
                                  }
                                }
                                """.formatted(trainingSessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value((int) trainingSessionId))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/training/sessions/{sessionId}/next-item", trainingSessionId)
                        .with(bearer(otherStudentToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/training/sessions/{sessionId}/progress", trainingSessionId)
                        .with(bearer(otherStudentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "progressSnapshot": {
                                    "sessionId": %d,
                                    "currentItemOrder": 1,
                                    "answeredItems": 0
                                  }
                                }
                                """.formatted(trainingSessionId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/training/sessions")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": %d,
                                  "mode": "FALSE_FRIEND_DISCRIM"
                                }
                                """.formatted(planId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        List<TrainingItemResultEntity> itemResults = trainingItemResultMapper.selectList(Wrappers.<TrainingItemResultEntity>lambdaQuery()
                .eq(TrainingItemResultEntity::getSessionId, trainingSessionId)
                .orderByAsc(TrainingItemResultEntity::getPresentationOrder));
        assertThat(itemResults).isNotEmpty();

        for (int index = 0; index < itemResults.size(); index++) {
            TrainingItemResultEntity itemResult = itemResults.get(index);
            mockMvc.perform(get("/api/training/sessions/{sessionId}/next-item", trainingSessionId)
                            .with(bearer(studentToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hasNextItem").value(true));

            String answerKey = index == 0 ? firstWrongOptionKey(itemResult) : itemResult.getCorrectAnswerKey();
            int reactionTime = index == 0 ? 1490 : 860;
            int hesitationTime = index == 0 ? 360 : 120;
            String clientRequestId = index == 0 ? "training-answer-first" : "training-answer-" + index;

            ResultActions submitAnswer = mockMvc.perform(post("/api/training/sessions/{sessionId}/answers", trainingSessionId)
                            .with(bearer(studentToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "itemResultId": %d,
                                      "clientRequestId": "%s",
                                      "selectedAnswerKey": "%s",
                                      "reactionTimeMs": %d,
                                      "hesitationTimeMs": %d
                                    }
                                    """.formatted(itemResult.getId(), clientRequestId, answerKey, reactionTime, hesitationTime)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.answeredItems").value(index + 1));
            if (index == itemResults.size() - 1) {
                submitAnswer.andExpect(jsonPath("$.data.status").value("COMPLETED"))
                        .andExpect(jsonPath("$.data.completed").value(true))
                        .andExpect(jsonPath("$.data.completionHooksStatus").value("PENDING"));
            } else {
                submitAnswer.andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                        .andExpect(jsonPath("$.data.completed").value(false));
            }

            if (index == 0) {
                mockMvc.perform(post("/api/training/sessions/{sessionId}/answers", trainingSessionId)
                                .with(bearer(studentToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "itemResultId": %d,
                                          "clientRequestId": "%s",
                                          "selectedAnswerKey": "%s",
                                          "reactionTimeMs": %d,
                                          "hesitationTimeMs": %d
                                        }
                                        """.formatted(itemResult.getId(), clientRequestId, answerKey, reactionTime, hesitationTime)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.data.answeredItems").value(1));

                mockMvc.perform(post("/api/training/sessions/{sessionId}/answers", trainingSessionId)
                                .with(bearer(studentToken))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "itemResultId": %d,
                                          "clientRequestId": "%s",
                                          "selectedAnswerKey": "%s",
                                          "reactionTimeMs": %d,
                                          "hesitationTimeMs": %d
                                        }
                                        """.formatted(itemResult.getId(), clientRequestId, itemResult.getCorrectAnswerKey(), reactionTime, hesitationTime)))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.message").value("clientRequestId already exists with a different answer payload"));
            }
        }

        mockMvc.perform(get("/api/training/sessions/{sessionId}/next-item", trainingSessionId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.hasNextItem").value(false))
                .andExpect(jsonPath("$.data.answeredItems").value(itemResults.size()))
                .andExpect(jsonPath("$.data.completionHooksStatus").isString());

        MvcResult followUpTrainingSessionResult = mockMvc.perform(post("/api/training/sessions")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": %d,
                                  "mode": "FALSE_FRIEND_DISCRIM"
                                }
                                """.formatted(planId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andReturn();
        long followUpTrainingSessionId = readJson(followUpTrainingSessionResult).path("data").path("sessionId").asLong();
        assertThat(followUpTrainingSessionId).isNotEqualTo(trainingSessionId);

        mockMvc.perform(post("/api/training/sessions/{sessionId}/complete", trainingSessionId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.completionHooksStatus").isString());

        assertThat(trainingSessionMapper.abandonIfInProgress(trainingSessionId, LocalDateTime.now())).isZero();

        mockMvc.perform(post("/api/training/sessions/{sessionId}/progress", trainingSessionId)
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "progressSnapshot": {
                                    "sessionId": %d,
                                    "currentItemOrder": 3,
                                    "answeredItems": 3
                                  }
                                }
                                """.formatted(trainingSessionId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Training session is already completed"));

        mockMvc.perform(get("/api/training/sessions/{sessionId}/summary", trainingSessionId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accuracy").isNumber())
                .andExpect(jsonPath("$.data.averageReactionTime").isNumber())
                .andExpect(jsonPath("$.data.improvementHint").isString())
                .andExpect(jsonPath("$.data.nextRecommendedMode").isString())
                .andExpect(jsonPath("$.data.riskWordsToReview.length()").value(1));

        mockMvc.perform(get("/api/training/wrong-book")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].wrongCount").value(1));

        mockMvc.perform(get("/api/training/review-schedule")
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

        assertThat(wrongBookMapper.selectCount(Wrappers.<com.huashi.eftransfer.app.modules.training.entity.WrongBookEntity>lambdaQuery())).isEqualTo(1);
        assertThat(reviewScheduleMapper.selectCount(Wrappers.<com.huashi.eftransfer.app.modules.training.entity.ReviewScheduleEntity>lambdaQuery())).isEqualTo(4);

        StudentProfileEntity studentProfile = studentProfileMapper.selectList(Wrappers.<StudentProfileEntity>lambdaQuery())
                .stream()
                .filter(profile -> profile.getLearningProfileSnapshotJson() != null && !profile.getLearningProfileSnapshotJson().isBlank())
                .findFirst()
                .orElse(null);
        assertThat(studentProfile).isNotNull();
        assertThat(studentProfile.getLearningProfileSnapshotJson()).isNotBlank();
        assertThat(studentProfile.getLearningProfileUpdatedAt()).isNotNull();
    }

    @Test
    void shouldHeartbeatActiveTrainingSessionsAndAutoCloseTimedOutSessions() throws Exception {
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
                      "englishDefinition": "a small piece of money",
                      "frenchDefinition": "angle ou recoin",
                      "chineseDefinition": "硬币；角落",
                      "examples": [
                        {
                          "sortOrder": 1,
                          "englishExample": "I found a coin on the floor.",
                          "frenchExample": "Il attend dans le coin de la piece.",
                          "chineseTranslation": "他站在房间的角落。",
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
                  "semanticOverlapScore": 0.20,
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
                      "englishDefinition": "in fact",
                      "frenchDefinition": "en ce moment",
                      "chineseDefinition": "实际上；目前",
                      "examples": [
                        {
                          "sortOrder": 1,
                          "englishExample": "I actually enjoyed it.",
                          "frenchExample": "Il travaille actuellement a Paris.",
                          "chineseTranslation": "他目前在巴黎工作。",
                          "contextSupportLevel": "high",
                          "source": "Teacher Curated"
                        }
                      ]
                    }
                  ]
                }
                """);
        long templateId = createDiagnosisTemplate(teacherToken, tablePairId, coinPairId, actuallyPairId);
        long diagnosisSessionId = createDiagnosisSessionAndComplete(studentToken, templateId);
        long diagnosisSummaryId = diagnosisSummaryMapper.selectOne(Wrappers.<DiagnosisSummaryEntity>lambdaQuery()
                        .eq(DiagnosisSummaryEntity::getSessionId, diagnosisSessionId))
                .getId();
        long planId = createRecommendedTrainingPlan(studentToken, diagnosisSummaryId);

        long heartbeatSessionId = startTrainingSession(studentToken, planId, "FALSE_FRIEND_DISCRIM");
        TrainingSessionEntity heartbeatSession = trainingSessionMapper.selectById(heartbeatSessionId);
        heartbeatSession.setLastSavedAt(LocalDateTime.now().minusHours(13));
        trainingSessionMapper.updateById(heartbeatSession);

        mockMvc.perform(post("/api/training/sessions/{sessionId}/heartbeat", heartbeatSessionId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.lastSavedAt").isNotEmpty());

        TrainingSessionEntity touchedSession = trainingSessionMapper.selectById(heartbeatSessionId);
        assertThat(touchedSession.getLastSavedAt()).isAfter(heartbeatSession.getLastSavedAt());
        trainingSessionMapper.abandonIfInProgress(heartbeatSessionId, LocalDateTime.now());

        long readySessionId = startTrainingSession(studentToken, planId, "FALSE_FRIEND_DISCRIM");
        answerAllTrainingItems(studentToken, readySessionId);
        TrainingSessionEntity readySession = trainingSessionMapper.selectById(readySessionId);
        readySession.setLastSavedAt(LocalDateTime.now().minusHours(13));
        trainingSessionMapper.updateById(readySession);

        assertThat(trainingSessionService.completeTimedOutReadySessions(LocalDateTime.now().minusHours(12), 10)).isEqualTo(1);
        assertThat(trainingSessionMapper.selectById(readySessionId).getStatus()).isEqualTo("COMPLETED");

        mockMvc.perform(post("/api/training/sessions/{sessionId}/heartbeat", readySessionId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/api/training/sessions/{sessionId}/summary", readySessionId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value((int) readySessionId));

        long abandonedSessionId = startTrainingSession(studentToken, planId, "FALSE_FRIEND_DISCRIM");
        TrainingSessionEntity abandonedSession = trainingSessionMapper.selectById(abandonedSessionId);
        abandonedSession.setLastSavedAt(LocalDateTime.now().minusHours(13));
        trainingSessionMapper.updateById(abandonedSession);

        assertThat(trainingSessionService.abandonTimedOutSessions(LocalDateTime.now().minusHours(12), 10)).isEqualTo(1);
        assertThat(trainingSessionMapper.selectById(abandonedSessionId).getStatus()).isEqualTo("ABANDONED");

        mockMvc.perform(post("/api/training/sessions/{sessionId}/heartbeat", abandonedSessionId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ABANDONED"));
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
        MvcResult templateCreateResult = mockMvc.perform(post("/api/teacher/diagnosis-templates")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateName": "Training linked diagnosis",
                                  "description": "Training recommendation source",
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
        return readJson(templateCreateResult).path("data").asLong();
    }

    private long createDiagnosisSessionAndComplete(String studentToken, long templateId) throws Exception {
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
                submitDiagnosisAnswer(studentToken, sessionId, itemResultId, """
                        {
                          "itemResultId": %d,
                          "selectedSemanticMatch": true,
                          "reactionTimeMs": 620,
                          "hesitationTimeMs": 90
                        }
                        """.formatted(itemResultId));
            } else if ("coin".equals(englishWord)) {
                submitDiagnosisAnswer(studentToken, sessionId, itemResultId, """
                        {
                          "itemResultId": %d,
                          "selectedSemanticMatch": true,
                          "reactionTimeMs": 1320,
                          "hesitationTimeMs": 360
                        }
                        """.formatted(itemResultId));
            } else {
                submitDiagnosisAnswer(studentToken, sessionId, itemResultId, """
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
        return sessionId;
    }

    private long createRecommendedTrainingPlan(String studentToken, long diagnosisSummaryId) throws Exception {
        MvcResult planResult = mockMvc.perform(get("/api/training/plans/recommended")
                        .with(bearer(studentToken))
                        .param("diagnosisSummaryId", String.valueOf(diagnosisSummaryId)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(planResult).path("data").path("planId").asLong();
    }

    private long startTrainingSession(String studentToken, long planId, String mode) throws Exception {
        MvcResult sessionStart = mockMvc.perform(post("/api/training/sessions")
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "planId": %d,
                                  "mode": "%s"
                                }
                                """.formatted(planId, mode)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(sessionStart).path("data").path("sessionId").asLong();
    }

    private void answerAllTrainingItems(String studentToken, long sessionId) throws Exception {
        List<TrainingItemResultEntity> itemResults = trainingItemResultMapper.selectList(Wrappers.<TrainingItemResultEntity>lambdaQuery()
                .eq(TrainingItemResultEntity::getSessionId, sessionId)
                .orderByAsc(TrainingItemResultEntity::getPresentationOrder)
                .orderByAsc(TrainingItemResultEntity::getId));
        for (int index = 0; index < itemResults.size(); index++) {
            MvcResult nextItemResult = mockMvc.perform(get("/api/training/sessions/{sessionId}/next-item", sessionId)
                            .with(bearer(studentToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.hasNextItem").value(true))
                    .andReturn();
            long itemResultId = readJson(nextItemResult).path("data").path("item").path("itemResultId").asLong();
            TrainingItemResultEntity itemResult = trainingItemResultMapper.selectById(itemResultId);
            String answerKey = index == 0 ? firstWrongOptionKey(itemResult) : itemResult.getCorrectAnswerKey();

            mockMvc.perform(post("/api/training/sessions/{sessionId}/answers", sessionId)
                            .with(bearer(studentToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "itemResultId": %d,
                                      "clientRequestId": "auto-timeout-%d",
                                      "selectedAnswerKey": "%s",
                                      "reactionTimeMs": %d,
                                      "hesitationTimeMs": %d
                                    }
                                    """.formatted(itemResultId, index + 1, answerKey, 720 + (index * 120), 120 + (index * 20))))
                    .andExpect(status().isOk());
        }
    }

    private void submitDiagnosisAnswer(String studentToken, long sessionId, long itemResultId, String body) throws Exception {
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
}
