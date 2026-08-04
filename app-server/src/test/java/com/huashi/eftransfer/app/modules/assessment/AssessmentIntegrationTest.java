package com.huashi.eftransfer.app.modules.assessment;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassMapper;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAttemptMapper;
import com.huashi.eftransfer.app.modules.assessment.service.AssessmentService;
import com.huashi.eftransfer.app.modules.notification.entity.NotificationEntity;
import com.huashi.eftransfer.app.modules.notification.mapper.NotificationMapper;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AssessmentIntegrationTest extends AbstractWebIntegrationTest {

    private static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired
    private TeachingClassMapper teachingClassMapper;

    @Autowired
    private AssessmentAttemptMapper assessmentAttemptMapper;

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private NotificationMapper notificationMapper;

    @Test
    void shouldAcceptAnyOneFillBlankAlternativeAndRejectMultipleResponses() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");

        long paperId = createPaper(teacherToken, "填空同义词测评", """
                {
                  "questionType": "FILL_BLANK",
                  "stemText": "填写一个可接受的法语问候语",
                  "promptText": "任写一个即可",
                  "correctAnswers": ["Bonjour", "salut"],
                  "explanationText": "两种答案均可接受",
                  "score": 10
                }
                """);
        long publishId = publishPaper(
                teacherToken,
                paperId,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1)
        );
        long attemptId = startAttempt(studentToken, publishId);

        mockMvc.perform(post("/api/student/assessments/attempts/{attemptId}/responses", attemptId)
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "responses": [
                                    {
                                      "questionOrder": 1,
                                      "responses": ["Bonjour", "salut"]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Fill blank question accepts only one response"));

        mockMvc.perform(post("/api/student/assessments/attempts/{attemptId}/responses", attemptId)
                        .with(bearer(studentToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "responses": [
                                    {
                                      "questionOrder": 1,
                                      "responses": ["salut"]
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answeredCount").value(1));

        mockMvc.perform(post("/api/student/assessments/attempts/{attemptId}/submit", attemptId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        mockMvc.perform(get("/api/student/assessments/attempts/{attemptId}/result", attemptId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.objectiveScore").value(10))
                .andExpect(jsonPath("$.data.questions[0].correct").value(true))
                .andExpect(jsonPath("$.data.questions[0].scoreAwarded").value(10));
    }

    @Test
    void shouldReturnSameAttemptAcrossConcurrentStarts() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");

        long paperId = createPaper(teacherToken, "并发开始测评", """
                {
                  "questionType": "SINGLE_CHOICE",
                  "stemText": "选择正确答案",
                  "promptText": "请选择一项",
                  "options": [
                    {"key": "A", "label": "正确答案"},
                    {"key": "B", "label": "错误答案"}
                  ],
                  "correctAnswers": ["A"],
                  "explanationText": "A 是正确答案",
                  "score": 10
                }
                """);
        long publishId = publishPaper(
                teacherToken,
                paperId,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1)
        );

        int requestCount = 8;
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        try {
            List<Future<MvcResult>> futures = java.util.stream.IntStream.range(0, requestCount)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        if (!start.await(5, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("timed out waiting to start concurrent requests");
                        }
                        return mockMvc.perform(post("/api/student/assessments/publishes/{publishId}/start", publishId)
                                        .with(bearer(studentToken)))
                                .andExpect(status().isOk())
                                .andReturn();
                    }))
                    .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Long> attemptIds = futures.stream()
                    .map(future -> {
                        try {
                            return readJson(future.get(10, TimeUnit.SECONDS)).path("data").path("attemptId").asLong();
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    })
                    .toList();

            assertThat(attemptIds).hasSize(requestCount);
            assertThat(attemptIds.stream().distinct()).hasSize(1);
            assertThat(assessmentAttemptMapper.selectCount(Wrappers.<AssessmentAttemptEntity>lambdaQuery()
                    .eq(AssessmentAttemptEntity::getPublishId, publishId))).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldExcludeFutureStartsFromTeacherWorkspaceSummary() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");

        long paperId = createPaper(teacherToken, "工作台统计测评", """
                {
                  "questionType": "SINGLE_CHOICE",
                  "stemText": "选择正确答案",
                  "promptText": "请选择一项",
                  "options": [
                    {"key": "A", "label": "正确答案"},
                    {"key": "B", "label": "错误答案"}
                  ],
                  "correctAnswers": ["A"],
                  "explanationText": "A 是正确答案",
                  "score": 10
                }
                """);
        long activePublishId = publishPaper(
                teacherToken,
                paperId,
                LocalDateTime.now().minusMinutes(10),
                LocalDateTime.now().plusHours(2)
        );
        publishPaper(
                teacherToken,
                paperId,
                LocalDateTime.now().plusHours(2),
                LocalDateTime.now().plusHours(4)
        );

        mockMvc.perform(get("/api/teacher/workspace/overview")
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.activeAssessmentPublishCount").value(1))
                .andExpect(jsonPath("$.data.summary.pendingAssessmentSubmissionCount").value(2))
                .andExpect(jsonPath("$.data.recentAssessmentPublishes.length()").value(1))
                .andExpect(jsonPath("$.data.recentAssessmentPublishes[0].publishId").value((int) activePublishId));
    }

    @Test
    void shouldAutoSubmitExpiredAttemptsFromBackendBatchAndExposeHeartbeatStatus() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");

        long paperId = createPaper(teacherToken, "超时自动交卷测评", """
                {
                  "questionType": "SINGLE_CHOICE",
                  "stemText": "选择正确答案",
                  "promptText": "请选择一项",
                  "options": [
                    {"key": "A", "label": "正确答案"},
                    {"key": "B", "label": "错误答案"}
                  ],
                  "correctAnswers": ["A"],
                  "explanationText": "A 是正确答案",
                  "score": 10
                }
                """);
        long publishId = publishPaper(
                teacherToken,
                paperId,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1)
        );
        long attemptId = startAttempt(studentToken, publishId);

        AssessmentAttemptEntity attempt = assessmentAttemptMapper.selectById(attemptId);
        attempt.setExpiresAt(LocalDateTime.now().minusSeconds(5));
        assessmentAttemptMapper.updateById(attempt);

        assertThat(assessmentService.submitExpiredAttemptsBatch(10)).isEqualTo(1);

        AssessmentAttemptEntity submittedAttempt = assessmentAttemptMapper.selectById(attemptId);
        assertThat(submittedAttempt.getStatus()).isEqualTo("SUBMITTED");
        assertThat(submittedAttempt.getSubmittedAt()).isNotNull();

        mockMvc.perform(get("/api/student/assessments/attempts/{attemptId}/heartbeat", attemptId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.submittedAt").isNotEmpty());

        assertThat(notificationMapper.selectCount(Wrappers.<NotificationEntity>lambdaQuery()
                .eq(NotificationEntity::getCategory, "ASSESSMENT_TIMEOUT_SUBMITTED")
                .like(NotificationEntity::getPayloadJson, "\"attemptId\":" + attemptId))).isEqualTo(1);
    }

    @Test
    void shouldTreatLateManualSubmitAsManualNotification() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        String studentToken = loginAndGetAccessToken("student.li", "Student@123456");

        long paperId = createPaper(teacherToken, "截止后手动交卷测评", """
                {
                  "questionType": "SINGLE_CHOICE",
                  "stemText": "选择正确答案",
                  "promptText": "请选择一项",
                  "options": [
                    {"key": "A", "label": "正确答案"},
                    {"key": "B", "label": "错误答案"}
                  ],
                  "correctAnswers": ["A"],
                  "explanationText": "A 是正确答案",
                  "score": 10
                }
                """);
        long publishId = publishPaper(
                teacherToken,
                paperId,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusHours(1)
        );
        long attemptId = startAttempt(studentToken, publishId);

        AssessmentAttemptEntity attempt = assessmentAttemptMapper.selectById(attemptId);
        attempt.setExpiresAt(LocalDateTime.now().minusSeconds(5));
        assessmentAttemptMapper.updateById(attempt);

        mockMvc.perform(post("/api/student/assessments/attempts/{attemptId}/submit", attemptId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

        AssessmentAttemptEntity submittedAttempt = assessmentAttemptMapper.selectById(attemptId);
        assertThat(submittedAttempt.getStatus()).isEqualTo("SUBMITTED");
        assertThat(submittedAttempt.getSubmittedAt()).isNotNull();

        assertThat(notificationMapper.selectCount(Wrappers.<NotificationEntity>lambdaQuery()
                .eq(NotificationEntity::getCategory, "ASSESSMENT_SUBMITTED")
                .like(NotificationEntity::getPayloadJson, "\"attemptId\":" + attemptId))).isEqualTo(1);
        assertThat(notificationMapper.selectCount(Wrappers.<NotificationEntity>lambdaQuery()
                .eq(NotificationEntity::getCategory, "ASSESSMENT_TIMEOUT_SUBMITTED")
                .like(NotificationEntity::getPayloadJson, "\"attemptId\":" + attemptId))).isEqualTo(0);
    }

    private long createPaper(String teacherToken, String title, String questionJson) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/assessments/papers")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "integration test paper",
                                  "durationMinutes": 30,
                                  "questions": [%s]
                                }
                                """.formatted(title, questionJson)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("paperId").asLong();
    }

    private long publishPaper(String teacherToken, long paperId, LocalDateTime startsAt, LocalDateTime dueAt) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/assessments/papers/{paperId}/publish", paperId)
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "teachingClassId": %d,
                                  "startsAt": "%s",
                                  "dueAt": "%s",
                                  "instructionsText": "请认真作答",
                                  "resultReleasePolicy": "IMMEDIATE"
                                }
                                """.formatted(loadClassId(), startsAt.format(ISO_TIME), dueAt.format(ISO_TIME))))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("publishId").asLong();
    }

    private long startAttempt(String studentToken, long publishId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/student/assessments/publishes/{publishId}/start", publishId)
                        .with(bearer(studentToken)))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data").path("attemptId").asLong();
    }

    private long loadClassId() {
        TeachingClassEntity teachingClass = teachingClassMapper.selectOne(Wrappers.<TeachingClassEntity>lambdaQuery()
                .eq(TeachingClassEntity::getClassCode, "CLS-0001")
                .last("LIMIT 1"));
        assertThat(teachingClass).isNotNull();
        return teachingClass.getId();
    }
}
