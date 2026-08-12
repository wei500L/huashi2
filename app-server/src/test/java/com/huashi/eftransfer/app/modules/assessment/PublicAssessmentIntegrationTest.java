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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import tools.jackson.databind.JsonNode;

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
    @Autowired private JdbcTemplate jdbcTemplate;

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
                .andExpect(jsonPath("$.data.questionCount").value(1))
                .andExpect(jsonPath("$.data.formalQuestionCount").value(1))
                .andExpect(jsonPath("$.data.profileFieldCount").value(0));

        MvcResult verified = mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participationCode":"%s"}
                                """.formatted(participationCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resumed").value(false))
                .andExpect(jsonPath("$.data.attempt.questions[0].formalSection").value(true))
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

    @Test
    void shouldHandleSpellingHintFlowAndKeepAnswerPrivateUntilSubmission() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long paperId = createSpellingPaper(teacherToken);
        MvcResult published = publishPublic(teacherToken, paperId);
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();
        String participationCode = readJson(published).path("data").path("participationCodes").get(0).asText();

        MvcResult verified = mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participationCode\":\"%s\"}".formatted(participationCode)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = sessionCookie(verified);

        MvcResult attemptPayload = mockMvc.perform(get("/api/public/assessments/{releaseCode}/attempt", releaseCode)
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andReturn();
        String attemptJson = attemptPayload.getResponse().getContentAsString();
        assertThat(attemptJson).doesNotContain("paradis");
        assertThat(attemptJson).doesNotContain("correctAnswer");

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/spelling-attempt", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionOrder\":1,\"candidate\":\"   \"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/timing", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionOrder\":1,\"activeDurationMs\":15000,\"eventId\":\"spelling-wrong-type\",\"eventType\":\"ACTIVE_DELTA\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/timing", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionOrder\":1,\"activeDurationMs\":15000,\"eventId\":\"spelling-pre-hint\",\"eventType\":\"SPELLING_PRE_HINT_DELTA\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/spelling-attempt", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionOrder\":1,\"candidate\":\"wrong-answer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.hintShown").value(true))
                .andExpect(jsonPath("$.data.hintFirstLetter").value("p"))
                .andExpect(jsonPath("$.data.wrongAttemptCount").value(1));

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/spelling-attempt", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionOrder\":1,\"candidate\":\"paradis\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correct").value(true))
                .andExpect(jsonPath("$.data.hintShown").value(true));

        MvcResult restored = mockMvc.perform(get("/api/public/assessments/{releaseCode}/attempt", releaseCode)
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andReturn();
        String restoredJson = restored.getResponse().getContentAsString();
        assertThat(restoredJson).contains("\"spellingHintShown\":true");
        assertThat(restoredJson).contains("\"spellingHintFirstLetter\":\"p\"");
        assertThat(restoredJson).contains("\"spellingWrongAttemptCount\":1");
        assertThat(restoredJson).doesNotContain("\"correctAnswer");
    }

    @Test
    void shouldRejectSpellingAttemptsOnNonSpellingQuestions() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long paperId = createPaper(teacherToken);
        MvcResult published = publishPublic(teacherToken, paperId);
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();
        String participationCode = readJson(published).path("data").path("participationCodes").get(0).asText();

        MvcResult verified = mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participationCode\":\"%s\"}".formatted(participationCode)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = sessionCookie(verified);

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/spelling-attempt", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"questionOrder\":1,\"candidate\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void oldAttemptKeepsItsFortyMinuteWindowWhenPublishDurationIsSyncedToSixty() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long paperId = createPaper(teacherToken, 40);
        MvcResult published = publishPublic(teacherToken, paperId);
        long publishId = readJson(published).path("data").path("publishId").asLong();
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();
        String participationCode = readJson(published).path("data").path("participationCodes").get(0).asText();

        MvcResult verified = mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participationCode\":\"%s\"}".formatted(participationCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attempt.durationMinutes").value(40))
                .andReturn();
        Cookie cookie = sessionCookie(verified);

        JsonNode baseline = attemptData(releaseCode, cookie);
        assertThat(baseline.path("durationMinutes").asInt()).isEqualTo(40);
        assertThat(baseline.path("questions").size()).isEqualTo(1);

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/responses", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"baseVersion\":1,\"responses\":[{\"questionOrder\":1,\"responses\":[\"T\"],\"justificationText\":null}]}"))
                .andExpect(status().isOk());

        JsonNode beforeSync = attemptData(releaseCode, cookie);
        assertThat(beforeSync.path("startedAt").asText()).isEqualTo(baseline.path("startedAt").asText());
        assertThat(beforeSync.path("expiresAt").asText()).isEqualTo(baseline.path("expiresAt").asText());
        assertThat(beforeSync.path("questions").get(0).path("responses").get(0).asText()).isEqualTo("T");

        jdbcTemplate.update("UPDATE assessment_publish SET duration_minutes = 60 WHERE id = ?", publishId);

        JsonNode afterSync = attemptData(releaseCode, cookie);
        assertThat(afterSync.path("startedAt").asText()).isEqualTo(beforeSync.path("startedAt").asText());
        assertThat(afterSync.path("expiresAt").asText()).isEqualTo(beforeSync.path("expiresAt").asText());
        assertThat(afterSync.path("durationMinutes").asInt()).isEqualTo(40);
        assertThat(java.time.Duration.between(LocalDateTime.parse(afterSync.path("startedAt").asText(), ISO_TIME),
                LocalDateTime.parse(afterSync.path("expiresAt").asText(), ISO_TIME))).isEqualTo(java.time.Duration.ofMinutes(40));
        assertThat(afterSync.path("attemptId").asLong()).isEqualTo(beforeSync.path("attemptId").asLong());
        assertThat(afterSync.path("questions").size()).isEqualTo(beforeSync.path("questions").size());
        assertThat(afterSync.path("questions").get(0).path("questionOrder").asInt())
                .isEqualTo(beforeSync.path("questions").get(0).path("questionOrder").asInt());
        assertThat(afterSync.path("questions").get(0).path("responses").get(0).asText()).isEqualTo("T");
    }

    @Test
    void attemptExpiringAtPublishDueAtReportsActualWindowInsteadOfConfiguredDuration() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long paperId = createPaper(teacherToken, 60);
        LocalDateTime dueAt = LocalDateTime.now().plusMinutes(20);
        MvcResult published = publishPublic(teacherToken, paperId, dueAt);
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();
        String participationCode = readJson(published).path("data").path("participationCodes").get(0).asText();

        MvcResult verified = mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participationCode\":\"%s\"}".formatted(participationCode)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = sessionCookie(verified);
        JsonNode attempt = readJson(verified).path("data").path("attempt");
        LocalDateTime startedAt = LocalDateTime.parse(attempt.path("startedAt").asText(), ISO_TIME);
        LocalDateTime expiresAt = LocalDateTime.parse(attempt.path("expiresAt").asText(), ISO_TIME);
        assertThat(java.time.Duration.between(dueAt, expiresAt).abs())
                .isLessThanOrEqualTo(java.time.Duration.ofSeconds(1));
        long seconds = java.time.Duration.between(startedAt, expiresAt).getSeconds();
        long expectedMinutes = Math.floorDiv(seconds, 60);
        if (seconds % 60 != 0) {
            expectedMinutes++;
        }
        assertThat(java.time.Duration.between(startedAt, expiresAt)).isGreaterThan(java.time.Duration.ZERO);
        assertThat(java.time.Duration.between(startedAt, expiresAt)).isLessThan(java.time.Duration.ofMinutes(60));
        assertThat(expiresAt).isBefore(startedAt.plusMinutes(60));
        assertThat(attempt.path("durationMinutes").asInt()).isEqualTo((int) expectedMinutes);
        assertThat(attempt.path("durationMinutes").asInt()).isLessThan(60);

        JsonNode restored = attemptData(releaseCode, cookie);
        assertThat(restored.path("durationMinutes").asInt()).isEqualTo(attempt.path("durationMinutes").asInt());
        assertThat(LocalDateTime.parse(restored.path("expiresAt").asText(), ISO_TIME).truncatedTo(ChronoUnit.SECONDS))
                .isEqualTo(expiresAt.truncatedTo(ChronoUnit.SECONDS));
        assertThat(java.time.Duration.between(LocalDateTime.parse(restored.path("startedAt").asText(), ISO_TIME),
                LocalDateTime.parse(restored.path("expiresAt").asText(), ISO_TIME)))
                .isCloseTo(java.time.Duration.between(startedAt, expiresAt), java.time.Duration.ofSeconds(1));
    }

    @Test
    void shouldCreateAttemptExpiringSixtyMinutesAfterStartWhenPublishDurationIsSixty() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long paperId = createPaper(teacherToken, 60);
        MvcResult published = publishPublic(teacherToken, paperId);
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();
        String participationCode = readJson(published).path("data").path("participationCodes").get(0).asText();

        MvcResult verified = mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participationCode\":\"%s\"}".formatted(participationCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attempt.durationMinutes").value(60))
                .andReturn();
        Cookie cookie = sessionCookie(verified);
        String startedAt = readJson(verified).path("data").path("attempt").path("startedAt").asText();
        String expiresAt = readJson(verified).path("data").path("attempt").path("expiresAt").asText();
        LocalDateTime start = LocalDateTime.parse(startedAt, ISO_TIME);
        LocalDateTime expiry = LocalDateTime.parse(expiresAt, ISO_TIME);
        assertThat(java.time.Duration.between(start, expiry)).isEqualTo(java.time.Duration.ofMinutes(60));

        mockMvc.perform(get("/api/public/assessments/{releaseCode}/attempt", releaseCode)
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.durationMinutes").value(60));
    }

    @Test
    void attemptHidesSoftDeletedQuestionsAndSubmitsWithoutRequiringThem() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long paperId = createPaper(teacherToken, 60, """
                {
                  "questionType":"TRUE_FALSE_WITH_JUSTIFICATION",
                  "stemText":"Kept statement",
                  "options":[{"key":"T","label":"True"},{"key":"F","label":"False"}],
                  "correctAnswers":["T"],
                  "explanationText":"T is correct",
                  "score":1,
                  "weight":1,
                  "transferCategory":"COGNATE",
                  "contextLevel":"WORD",
                  "constructCode":"LEXICAL_RECOGNITION"
                },
                {
                  "questionType":"TRUE_FALSE_WITH_JUSTIFICATION",
                  "stemText":"To be removed statement",
                  "options":[{"key":"T","label":"True"},{"key":"F","label":"False"}],
                  "correctAnswers":["T"],
                  "explanationText":"T is correct",
                  "score":1,
                  "weight":1,
                  "transferCategory":"COGNATE",
                  "contextLevel":"WORD",
                  "constructCode":"LEXICAL_RECOGNITION"
                }
                """);
        MvcResult published = publishPublic(teacherToken, paperId);
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();
        String participationCode = readJson(published).path("data").path("participationCodes").get(0).asText();

        MvcResult verified = mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participationCode\":\"%s\"}".formatted(participationCode)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attempt.questions.length()").value(2))
                .andReturn();
        Cookie cookie = sessionCookie(verified);

        long removedQuestionId = jdbcTemplate.queryForObject(
                "SELECT id FROM assessment_question WHERE paper_id = ? AND sort_order = 2 AND deleted = FALSE",
                Long.class, paperId);
        jdbcTemplate.update("UPDATE assessment_question SET deleted = TRUE WHERE id = ?", removedQuestionId);

        JsonNode restored = attemptData(releaseCode, cookie);
        assertThat(restored.path("questions").size()).isEqualTo(1);
        assertThat(restored.path("questions").get(0).path("questionOrder").asInt()).isEqualTo(1);

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/responses", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"baseVersion\":1,\"responses\":[{\"questionOrder\":1,\"responses\":[\"T\"]}]}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/submit", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"baseVersion\":2,\"reason\":\"MANUAL\",\"responses\":[{\"questionOrder\":1,\"responses\":[\"T\"]}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    private long createSpellingPaper(String teacherToken) throws Exception {
        return createPaper(teacherToken, 40, """
                                {
                                  "questionType":"SPELLING",
                                  "stemText":"天堂；乐土 ______（填写对应法语单词）",
                                  "correctAnswers":["paradis"],
                                  "explanationText":"paradis",
                                  "score":1,
                                  "weight":1,
                                  "transferCategory":"FALSE_FRIEND",
                                  "contextLevel":"WORD",
                                  "constructCode":"FF4_SPELLING"
                                }
                                """);
    }

    private long createPaper(String teacherToken) throws Exception {
        return createPaper(teacherToken, 40, """
                                {
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
                                }
                                """);
    }

    private long createPaper(String teacherToken, int durationMinutes) throws Exception {
        return createPaper(teacherToken, durationMinutes, """
                                {
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
                                }
                                """);
    }

    private long createPaper(String teacherToken, int durationMinutes, String questionJson) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/assessments/papers")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Public research integration",
                                  "paperPurpose":"RESEARCH_SURVEY",
                                  "durationMinutes":%d,
                                  "questions":[%s]
                                }
                                """.formatted(durationMinutes, questionJson)))
                .andExpect(status().isOk()).andReturn();
        return readJson(result).path("data").path("paperId").asLong();
    }

    private MvcResult publishPublic(String teacherToken, long paperId) throws Exception {
        return publishPublic(teacherToken, paperId, LocalDateTime.now().plusHours(2));
    }

    private MvcResult publishPublic(String teacherToken, long paperId, LocalDateTime dueAt) throws Exception {
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
                                dueAt.format(ISO_TIME))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participationCodes.length()").value(2))
                .andReturn();
    }

    private JsonNode attemptData(String releaseCode, Cookie cookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/public/assessments/{releaseCode}/attempt", releaseCode)
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private Cookie sessionCookie(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotBlank();
        String value = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
        return new Cookie("LEXIBRIDGE_SESSION", value);
    }
}
