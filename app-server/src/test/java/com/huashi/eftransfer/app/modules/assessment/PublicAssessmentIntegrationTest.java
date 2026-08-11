package com.huashi.eftransfer.app.modules.assessment;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptAnswerEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAiAnalysisEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentMetricSnapshotEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipantEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentTimingEventEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipantAccessEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipationCodeEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAiAnalysisMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAttemptAnswerMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentMetricSnapshotMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipantMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentTimingEventMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipantAccessMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipationCodeMapper;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentParticipantCodeCodec;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

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
    @Autowired private AssessmentParticipantMapper participantMapper;
    @Autowired private AssessmentParticipantCodeCodec participantCodeCodec;
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

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/timing", releaseCode)
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionOrder":1,"activeDurationMs":45000,"eventId":"event-too-large"}
                                """))
                .andExpect(status().isBadRequest());

        String timingPayload = """
                {"questionOrder":1,"activeDurationMs":30000,"eventId":"event-1"}
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
    void shouldRequireEncryptedV2ProfileBeforeCreatingSixtyQuestionAttempt() throws Exception {
        V2Release release = activateV2Release();

        MvcResult metadata = mockMvc.perform(get("/api/public/assessments/{releaseCode}", release.releaseCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionCount").value(60))
                .andExpect(jsonPath("$.data.profileFields.length()").value(12))
                .andExpect(jsonPath("$.data.profileFields[?(@.itemCode == 'BASIC-TEM4')].displayCondition.fieldCode")
                        .value("BASIC-ENGLISH-MAJOR"))
                .andReturn();
        assertThat(metadata.getResponse().getContentAsString()).contains("姓名和联系方式").doesNotContain("匿名保存");

        MvcResult verified = verifyV2(release);
        Cookie cookie = sessionCookie(verified);
        assertThat(readJson(verified).path("data").path("profileRequired").asBoolean()).isTrue();
        assertThat(readJson(verified).path("data").path("attempt").isNull()).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assessment_attempt WHERE publish_id = ?", Long.class, release.publishId())).isZero();

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/profile", release.releaseCode())
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"consentAccepted":false,"values":{"BASIC-NAME":"Alice","BASIC-ENGLISH-MAJOR":"NON_ENGLISH_MAJOR"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Research participation consent is required"));

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/profile", release.releaseCode())
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"consentAccepted":true,"values":{"BASIC-NAME":"Alice","BASIC-ENGLISH-MAJOR":"NON_ENGLISH_MAJOR","BASIC-TEM4":520}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Participant profile field is not available for the selected branch: BASIC-TEM4"));

        MvcResult completed = mockMvc.perform(post("/api/public/assessments/{releaseCode}/profile", release.releaseCode())
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "consentAccepted":true,
                                  "values":{
                                    "BASIC-NAME":"Alice Test",
                                    "BASIC-CONTACT":"alice@example.test",
                                    "BASIC-ENGLISH-MAJOR":"NON_ENGLISH_MAJOR",
                                    "BASIC-CET4":510
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questionCount").value(60))
                .andExpect(jsonPath("$.data.questions.length()").value(60))
                .andExpect(jsonPath("$.data.activeElapsedMs").value(0))
                .andReturn();

        JsonNode attempt = readJson(completed).path("data");
        assertThat(completed.getResponse().getContentAsString())
                .doesNotContain("Alice Test", "alice@example.test", "BASIC-NAME", "BASIC-CONTACT");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assessment_attempt_answer WHERE attempt_id = ?", Long.class,
                attempt.path("attemptId").asLong())).isEqualTo(60L);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM assessment_attempt_answer aa
                JOIN assessment_questionnaire_item qi ON qi.assessment_question_id = aa.question_id
                JOIN assessment_questionnaire_section qs ON qs.id = qi.section_id
                WHERE aa.attempt_id = ? AND qs.formal_section = FALSE
                """, Long.class, attempt.path("attemptId").asLong())).isZero();

        AssessmentParticipantEntity participant = participantMapper.selectOne(
                Wrappers.<AssessmentParticipantEntity>lambdaQuery()
                        .eq(AssessmentParticipantEntity::getPublishId, release.publishId()).last("LIMIT 1"));
        assertThat(participant.getSensitiveProfileCiphertext()).isNotBlank()
                .doesNotContain("Alice Test", "alice@example.test");
        assertThat(participant.getSensitiveProfileIv()).isNotBlank();
        assertThat(participant.getSensitiveProfileKeyVersion()).isNotBlank();
        assertThat(participant.getConsentedAt()).isNotNull();

        JsonNode synonym = questionByItemCode(attempt, "P1B-05");
        JsonNode antonym = questionByItemCode(attempt, "P1B-06");
        assertThat(synonym.path("sectionInstruction").asText()).contains("意思相同");
        assertThat(antonym.path("sectionInstruction").asText()).contains("意思相反");
        assertThat(questionByItemCode(attempt, "P2-01").path("presentation").path("emphasis").get(0).path("bold").asBoolean()).isTrue();
        assertThat(questionByItemCode(attempt, "P2-01").path("presentation").path("emphasis").get(0).path("underline").asBoolean()).isTrue();
        for (JsonNode question : attempt.path("questions")) {
            String sectionCode = question.path("sectionCode").asText();
            if (sectionCode.equals("P3") || sectionCode.startsWith("P4")) {
                assertThat(question.path("sharedMaterial").asText()).isNotBlank();
            }
        }

        String timingPayload = """
                {"questionOrder":1,"activeDurationMs":1250,"eventId":"v2-active-1"}
                """;
        mockMvc.perform(post("/api/public/assessments/{releaseCode}/timing", release.releaseCode())
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(timingPayload))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/public/assessments/{releaseCode}/timing", release.releaseCode())
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(timingPayload))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/public/assessments/{releaseCode}/attempt", release.releaseCode()).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeElapsedMs").value(1250));
    }

    @Test
    void shouldAcceptTemScoresForEnglishMajorProfile() throws Exception {
        V2Release release = activateV2Release();
        Cookie cookie = sessionCookie(verifyV2(release));

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/profile", release.releaseCode())
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "consentAccepted":true,
                                  "values":{
                                    "BASIC-NAME":"English Major",
                                    "BASIC-ENGLISH-MAJOR":"ENGLISH_MAJOR",
                                    "BASIC-CET4":530,
                                    "BASIC-CET6":510,
                                    "BASIC-TEM4":70,
                                    "BASIC-TEM8":65
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions.length()").value(60));
    }

    @Test
    void shouldAllowIdempotentTimeoutSubmissionAfterAttemptExpiry() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        MvcResult published = publishPublic(teacherToken, createPaper(teacherToken));
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();
        String participationCode = readJson(published).path("data").path("participationCodes").get(0).asText();
        MvcResult verified = mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participationCode":"%s"}
                                """.formatted(participationCode)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = sessionCookie(verified);
        long attemptId = readJson(verified).path("data").path("attempt").path("attemptId").asLong();
        jdbcTemplate.update("UPDATE assessment_attempt SET expires_at = ? WHERE id = ?",
                LocalDateTime.now().minusSeconds(1), attemptId);

        String timeoutPayload = """
                {"baseVersion":1,"reason":"TIMEOUT","responses":[{"questionOrder":1,"responses":["T"]}]}
                """;
        mockMvc.perform(post("/api/public/assessments/{releaseCode}/submit", releaseCode)
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(timeoutPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.submitReason").value("TIMEOUT"))
                .andExpect(jsonPath("$.data.version").value(2));
        mockMvc.perform(post("/api/public/assessments/{releaseCode}/submit", releaseCode)
                        .cookie(cookie).contentType(MediaType.APPLICATION_JSON).content(timeoutPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.submitReason").value("TIMEOUT"))
                .andExpect(jsonPath("$.data.version").value(2));
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

    private V2Release activateV2Release() {
        Long paperId = jdbcTemplate.queryForObject(
                "SELECT id FROM assessment_paper WHERE paper_code = 'LEXIBRIDGE_RESEARCH_V2' AND deleted = FALSE",
                Long.class);
        Long ownerId = jdbcTemplate.queryForObject(
                "SELECT owner_user_id FROM assessment_paper WHERE id = ?", Long.class, paperId);
        jdbcTemplate.update("""
                INSERT INTO assessment_publish
                    (paper_id,delivery_mode,published_by,status,paper_title_snapshot,paper_description_snapshot,
                     question_count_snapshot,total_score_snapshot,duration_minutes,instructions_text,starts_at,due_at,
                     result_release_policy,published_at,created_by,updated_by)
                SELECT id,'PUBLIC_CODE',?,'PUBLISHED',title,description,question_count,total_score,duration_minutes,
                       'Lexi-Bridge V2 integration release',?,?, 'IMMEDIATE',CURRENT_TIMESTAMP,?,?
                FROM assessment_paper WHERE id = ?
                """, ownerId, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1), ownerId, ownerId, paperId);
        Long publishId = jdbcTemplate.queryForObject(
                "SELECT id FROM assessment_publish WHERE paper_id = ? ORDER BY id DESC LIMIT 1", Long.class, paperId);
        String releaseCode = "RES-V2-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        jdbcTemplate.update("""
                INSERT INTO assessment_public_release
                    (publish_id,release_code,code_count,session_ttl_hours,qr_entry_enabled,status,created_by,updated_by)
                VALUES (?,?,1,12,FALSE,'PUBLISHED',?,?)
                """, publishId, releaseCode, ownerId, ownerId);
        Long publicReleaseId = jdbcTemplate.queryForObject(
                "SELECT id FROM assessment_public_release WHERE release_code = ?", Long.class, releaseCode);
        String participationCode = participantCodeCodec.generate();
        jdbcTemplate.update("""
                INSERT INTO assessment_participation_code
                    (public_release_id,code_digest,code_hint,status,export_batch_id,exported_at,created_by,updated_by)
                VALUES (?,?,?,'UNUSED','integration-v2',CURRENT_TIMESTAMP,?,?)
                """, publicReleaseId, participantCodeCodec.digest(participationCode),
                participationCode.substring(participationCode.length() - 4), ownerId, ownerId);
        return new V2Release(releaseCode, participationCode, publishId);
    }

    private MvcResult verifyV2(V2Release release) throws Exception {
        return mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", release.releaseCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"participationCode":"%s"}
                                """.formatted(release.participationCode())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileRequired").value(true))
                .andExpect(jsonPath("$.data.attempt").isEmpty())
                .andReturn();
    }

    private JsonNode questionByItemCode(JsonNode attempt, String itemCode) {
        for (JsonNode question : attempt.path("questions")) {
            if (itemCode.equals(question.path("itemCode").asText())) return question;
        }
        throw new AssertionError("Question was not found: " + itemCode);
    }

    private Cookie sessionCookie(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).isNotBlank();
        String value = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
        return new Cookie("LEXIBRIDGE_SESSION", value);
    }

    private record V2Release(String releaseCode, String participationCode, Long publishId) { }
}
