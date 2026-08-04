package com.huashi.eftransfer.app.modules.assessment;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptAnswerEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAiAnalysisEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentMetricSnapshotEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentTimingEventEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAiAnalysisMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAttemptAnswerMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentMetricSnapshotMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentTimingEventMapper;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicAssessmentIntegrationTest extends AbstractWebIntegrationTest {

    private static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired private AssessmentTimingEventMapper timingEventMapper;
    @Autowired private AssessmentAttemptAnswerMapper answerMapper;
    @Autowired private AssessmentMetricSnapshotMapper metricSnapshotMapper;
    @Autowired private AssessmentAiAnalysisMapper aiAnalysisMapper;

    @Test
    void shouldCompletePublicQuestionnaireWithIdempotentTimingAndSubmission() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long paperId = createPaper(teacherToken);
        MvcResult published = publishPublic(teacherToken, paperId);
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();
        String participationCode = readJson(published).path("data").path("participationCodes").get(0).asText();

        mockMvc.perform(get("/api/public/assessments/{releaseCode}", releaseCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionCount").value(1));

        MvcResult verified = mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participationCode":"%s"}
                                """.formatted(participationCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumed").value(false))
                .andReturn();
        Cookie cookie = sessionCookie(verified);
        long attemptId = readJson(verified).path("data").path("attempt").path("attemptId").asLong();

        mockMvc.perform(get("/api/public/assessments/{releaseCode}/attempt", releaseCode).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attemptId").value((int) attemptId));

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/responses", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseVersion":1,"responses":[{"questionOrder":1,"responses":["F"],"justificationText":"context suggests false"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2));

        String timingPayload = """
                {"questionOrder":1,"activeDurationMs":45000,"eventId":"event-1"}
                """;
        mockMvc.perform(post("/api/public/assessments/{releaseCode}/timing", releaseCode)
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(timingPayload))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/public/assessments/{releaseCode}/timing", releaseCode)
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(timingPayload))
                .andExpect(status().isOk());

        assertThat(timingEventMapper.selectCount(Wrappers.<AssessmentTimingEventEntity>lambdaQuery()
                .eq(AssessmentTimingEventEntity::getAttemptId, attemptId))).isEqualTo(1);
        AssessmentAttemptAnswerEntity timedAnswer = answerMapper.selectOne(
                Wrappers.<AssessmentAttemptAnswerEntity>lambdaQuery()
                        .eq(AssessmentAttemptAnswerEntity::getAttemptId, attemptId).last("LIMIT 1"));
        assertThat(timedAnswer.getEffectiveDurationMs()).isEqualTo(30_000L);

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/submit", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseVersion":2,"reason":"MANUAL","responses":[{"questionOrder":1,"responses":["F"]}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("A justification is required when a true/false answer is F"));

        String submitPayload = """
                {"baseVersion":2,"reason":"MANUAL","responses":[{"questionOrder":1,"responses":["T"]}]}
                """;
        mockMvc.perform(post("/api/public/assessments/{releaseCode}/submit", releaseCode)
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(submitPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.version").value(3));
        mockMvc.perform(post("/api/public/assessments/{releaseCode}/submit", releaseCode)
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(submitPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.version").value(3));

        mockMvc.perform(get("/api/public/assessments/{releaseCode}/result", releaseCode).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correctCount").value(1))
                .andExpect(jsonPath("$.data.metricSnapshot.scoringVersion").value("SCORING_V1"))
                .andExpect(jsonPath("$.data.aiAnalysisStatus").value("PENDING"))
                .andExpect(jsonPath("$.data.questions[0].justificationText").isEmpty());

        assertThat(metricSnapshotMapper.selectCount(Wrappers.<AssessmentMetricSnapshotEntity>lambdaQuery()
                .eq(AssessmentMetricSnapshotEntity::getAttemptId, attemptId))).isEqualTo(1);
        assertThat(aiAnalysisMapper.selectCount(Wrappers.<AssessmentAiAnalysisEntity>lambdaQuery()
                .eq(AssessmentAiAnalysisEntity::getAttemptId, attemptId))).isEqualTo(1);

        MvcResult resumed = mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participationCode":"%s"}
                                """.formatted(participationCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumed").value(true))
                .andExpect(jsonPath("$.data.attempt.attemptId").value((int) attemptId))
                .andReturn();
        assertThat(sessionCookie(resumed).getValue()).isNotBlank();
    }

    private long createPaper(String teacherToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/assessments/papers")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Public research integration",
                                  "durationMinutes":40,
                                  "questions":[{
                                    "questionType":"TRUE_FALSE_WITH_JUSTIFICATION",
                                    "stemText":"The statement is correct",
                                    "options":[{"key":"T","label":"True"},{"key":"F","label":"False"}],
                                    "correctAnswers":["T"],
                                    "explanationText":"T is correct",
                                    "score":1,
                                    "weight":1,
                                    "transferCategory":"COGNATE",
                                    "contextLevel":"WORD",
                                    "constructCode":"LEXICAL_RECOGNITION"
                                  }]
                                }
                                """))
                .andExpect(status().isOk()).andReturn();
        return readJson(result).path("data").path("paperId").asLong();
    }

    private MvcResult publishPublic(String teacherToken, long paperId) throws Exception {
        return mockMvc.perform(post("/api/teacher/assessments/papers/{paperId}/publish", paperId)
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deliveryMode":"PUBLIC_CODE",
                                  "participantCodeCount":2,
                                  "startsAt":"%s",
                                  "dueAt":"%s",
                                  "resultReleasePolicy":"IMMEDIATE"
                                }
                                """.formatted(LocalDateTime.now().minusMinutes(1).format(ISO_TIME),
                                LocalDateTime.now().plusHours(1).format(ISO_TIME))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participationCodes.length()").value(2))
                .andReturn();
    }

    private Cookie sessionCookie(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotBlank();
        String value = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
        return new Cookie("LEXIBRIDGE_SESSION", value);
    }
}
