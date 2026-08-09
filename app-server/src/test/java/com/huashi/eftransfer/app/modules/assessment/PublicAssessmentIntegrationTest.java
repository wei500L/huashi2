package com.huashi.eftransfer.app.modules.assessment;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptAnswerEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAiAnalysisEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentMetricSnapshotEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentTimingEventEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipantAccessEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipationCodeEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAiAnalysisMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAttemptAnswerMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentMetricSnapshotMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentTimingEventMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipantAccessMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipationCodeMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicAssessmentIntegrationTest extends AbstractWebIntegrationTest {

    private static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired private AssessmentTimingEventMapper timingEventMapper;
    @Autowired private AssessmentAttemptAnswerMapper answerMapper;
    @Autowired private AssessmentMetricSnapshotMapper metricSnapshotMapper;
    @Autowired private AssessmentAiAnalysisMapper aiAnalysisMapper;
    @Autowired private AssessmentParticipantAccessMapper participantAccessMapper;
    @Autowired private AssessmentParticipationCodeMapper participationCodeMapper;

    @Test
    void shouldReturnSpecificErrorForInvalidOrRevokedParticipationCode() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long paperId = createPaper(teacherToken);
        MvcResult published = publishPublic(teacherToken, paperId);
        long publishId = readJson(published).path("data").path("publishId").asLong();
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participationCode\":\"AAAA-BBBB-CCCC\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PARTICIPATION_CODE_INVALID"));

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participationCode\":\"not-a-valid-code\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PARTICIPATION_CODE_INVALID"));

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participationCode\":\"\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PARTICIPATION_CODE_INVALID"));

        MvcResult batch = mockMvc.perform(post("/api/teacher/assessments/publishes/{publishId}/participation-code-batches", publishId)
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participationCodes.length()").value(1))
                .andReturn();
        String batchId = readJson(batch).path("data").path("batchId").asText();
        String generatedCode = readJson(batch).path("data").path("participationCodes").get(0).asText();

        MvcResult listed = mockMvc.perform(get("/api/teacher/assessments/publishes/{publishId}/participation-codes", publishId)
                        .with(bearer(teacherToken)).param("batchId", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andReturn();
        long codeId = readJson(listed).path("data").path("records").get(0).path("codeId").asLong();

        mockMvc.perform(post("/api/teacher/assessments/publishes/{publishId}/participation-codes/{codeId}/revoke", publishId, codeId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revokedCount").value(1));

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participationCode\":\"%s\"}".formatted(generatedCode)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PARTICIPATION_CODE_INVALID"));

        mockMvc.perform(post("/api/teacher/assessments/publishes/{publishId}/participation-code-batches", publishId)
                        .with(bearer(teacherToken)).contentType(MediaType.APPLICATION_JSON).content("{\"count\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/teacher/assessments/publishes/{publishId}/participation-code-batches", publishId)
                        .with(bearer(teacherToken)).contentType(MediaType.APPLICATION_JSON).content("{\"count\":5001}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAuditManualCodeIpAndResumeQrParticipantAcrossIpChanges() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long paperId = createPaper(teacherToken);
        MvcResult published = publishPublic(teacherToken, paperId);
        long publishId = readJson(published).path("data").path("publishId").asLong();
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();
        String participationCode = readJson(published).path("data").path("participationCodes").get(0).asText();

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/qr-entry", releaseCode)
                        .header("X-Forwarded-For", "198.51.100.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"browserFingerprint\":\"0123456789abcdef0123456789abcdef\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/teacher/assessments/publishes/{publishId}/public-release", publishId)
                        .with(bearer(teacherToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .header("X-Forwarded-For", "203.0.113.18")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participationCode\":\"%s\"}".formatted(participationCode)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/teacher/assessments/publishes/{publishId}/participation-codes", publishId)
                        .with(bearer(teacherToken)).param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].lastVerifiedIp").value("203.0.113.18"));

        mockMvc.perform(patch("/api/teacher/assessments/publishes/{publishId}/public-release", publishId)
                        .with(bearer(teacherToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrEntryEnabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.qrEntryEnabled").value(true));

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/qr-entry", releaseCode)
                        .header("X-Forwarded-For", "not-an-ip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"browserFingerprint\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"}"))
                .andExpect(status().isBadRequest());

        String fingerprint = "0123456789abcdef0123456789abcdef";
        MvcResult firstQr = mockMvc.perform(post("/api/public/assessments/{releaseCode}/qr-entry", releaseCode)
                        .header("X-Forwarded-For", "198.51.100.10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"browserFingerprint\":\"%s\"}".formatted(fingerprint)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumed").value(false))
                .andReturn();
        long firstAttemptId = readJson(firstQr).path("data").path("attempt").path("attemptId").asLong();

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/qr-entry", releaseCode)
                        .header("X-Forwarded-For", "198.51.100.25")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"browserFingerprint\":\"%s\"}".formatted(fingerprint)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumed").value(true))
                .andExpect(jsonPath("$.data.attempt.attemptId").value((int) firstAttemptId));

        MvcResult secondDevice = mockMvc.perform(post("/api/public/assessments/{releaseCode}/qr-entry", releaseCode)
                        .header("X-Forwarded-For", "198.51.100.25")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"browserFingerprint\":\"fedcba9876543210fedcba9876543210\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumed").value(false))
                .andReturn();
        assertThat(readJson(secondDevice).path("data").path("attempt").path("attemptId").asLong())
                .isNotEqualTo(firstAttemptId);
        assertThat(participantAccessMapper.selectCount(Wrappers.<AssessmentParticipantAccessEntity>lambdaQuery()))
                .isGreaterThanOrEqualTo(4);
    }

    @Test
    void shouldPageLegacyCodesAndRevokeOnlyUnusedCodesInBatch() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long paperId = createPaper(teacherToken);
        MvcResult published = publishPublic(teacherToken, paperId);
        long publishId = readJson(published).path("data").path("publishId").asLong();
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();

        MvcResult initialCodes = mockMvc.perform(get("/api/teacher/assessments/publishes/{publishId}/participation-codes", publishId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andReturn();
        long legacyCodeId = readJson(initialCodes).path("data").path("records").get(0).path("codeId").asLong();
        participationCodeMapper.update(null, Wrappers.<AssessmentParticipationCodeEntity>lambdaUpdate()
                .set(AssessmentParticipationCodeEntity::getExportBatchId, null)
                .eq(AssessmentParticipationCodeEntity::getId, legacyCodeId));

        mockMvc.perform(get("/api/teacher/assessments/publishes/{publishId}/participation-codes", publishId)
                        .with(bearer(teacherToken)).param("batchId", "legacy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].exportBatchId").isEmpty());

        MvcResult batch = mockMvc.perform(post("/api/teacher/assessments/publishes/{publishId}/participation-code-batches", publishId)
                        .with(bearer(teacherToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":2}"))
                .andExpect(status().isOk())
                .andReturn();
        String batchId = readJson(batch).path("data").path("batchId").asText();
        String firstCode = readJson(batch).path("data").path("participationCodes").get(0).asText();

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .header("X-Forwarded-For", "2001:db8::42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participationCode\":\"%s\"}".formatted(firstCode)))
                .andExpect(status().isOk());

        MvcResult listed = mockMvc.perform(get("/api/teacher/assessments/publishes/{publishId}/participation-codes", publishId)
                        .with(bearer(teacherToken)).param("batchId", batchId).param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].lastVerifiedIp").value("2001:db8:0:0:0:0:0:42"))
                .andReturn();
        long inProgressCodeId = readJson(listed).path("data").path("records").get(0).path("codeId").asLong();

        mockMvc.perform(post("/api/teacher/assessments/publishes/{publishId}/participation-codes/{codeId}/revoke",
                        publishId, inProgressCodeId).with(bearer(teacherToken)))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/teacher/assessments/publishes/{publishId}/participation-code-batches/{batchId}/revoke-unused",
                        publishId, batchId).with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revokedCount").value(1));

        MvcResult finalBatchState = mockMvc.perform(get("/api/teacher/assessments/publishes/{publishId}/participation-codes", publishId)
                        .with(bearer(teacherToken)).param("batchId", batchId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andReturn();
        var records = readJson(finalBatchState).path("data").path("records");
        long inProgressCount = 0;
        long revokedCount = 0;
        for (var record : records) {
            if ("IN_PROGRESS".equals(record.path("status").asText())) {
                inProgressCount++;
            } else if ("REVOKED".equals(record.path("status").asText())) {
                revokedCount++;
            }
        }
        assertThat(inProgressCount).isEqualTo(1);
        assertThat(revokedCount).isEqualTo(1);
    }

    @Test
    void shouldGenerateAndPageMaximumParticipationCodeBatch() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long paperId = createPaper(teacherToken);
        MvcResult published = publishPublic(teacherToken, paperId);
        long publishId = readJson(published).path("data").path("publishId").asLong();

        MvcResult batch = mockMvc.perform(post("/api/teacher/assessments/publishes/{publishId}/participation-code-batches", publishId)
                        .with(bearer(teacherToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"count\":5000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participationCodes.length()").value(5000))
                .andReturn();
        String batchId = readJson(batch).path("data").path("batchId").asText();

        mockMvc.perform(get("/api/teacher/assessments/publishes/{publishId}/participation-codes", publishId)
                        .with(bearer(teacherToken)).param("batchId", batchId).param("pageNo", "2").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(5000))
                .andExpect(jsonPath("$.data.pageNo").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(100));
    }

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
                                  "paperPurpose":"RESEARCH_SURVEY",
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
