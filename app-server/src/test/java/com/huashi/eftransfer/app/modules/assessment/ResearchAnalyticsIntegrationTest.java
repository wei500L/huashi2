package com.huashi.eftransfer.app.modules.assessment;

import com.huashi.eftransfer.app.modules.assessment.entity.ResearchAiReportEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.ResearchAiReportMapper;
import com.huashi.eftransfer.app.modules.assessment.service.ResearchAiReportProcessor;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResearchAnalyticsIntegrationTest extends AbstractWebIntegrationTest {

    private static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Autowired private ResearchAiReportMapper reportMapper;
    @Autowired private ResearchAiReportProcessor reportProcessor;

    @Test
    void teacherCanInspectPublicAttemptsWithoutTeachingClassAndOthersAreForbidden() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        String outsiderToken = loginAndGetAccessToken("student.li", "Student@123456");
        long paperId = createPaper(teacherToken);
        MvcResult published = publishPublic(teacherToken, paperId);
        long publishId = readJson(published).path("data").path("publishId").asLong();
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();
        String participationCode = readJson(published).path("data").path("participationCodes").get(0).asText();

        mockMvc.perform(get("/api/teacher/research/releases").with(bearer(outsiderToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/teacher/research/publishes/{publishId}/overview", publishId).with(bearer(outsiderToken)))
                .andExpect(status().isForbidden());

        Cookie cookie = enterByCode(releaseCode, participationCode);
        JsonNode attempt = attemptData(releaseCode, cookie);
        long attemptId = attempt.path("attemptId").asLong();
        mockMvc.perform(post("/api/public/assessments/{releaseCode}/responses", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseVersion":%d,"responses":[{"questionOrder":1,"responses":["T"],"justificationText":"ok"}]}
                                """.formatted(attempt.path("version").asLong())))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/public/assessments/{releaseCode}/submit", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseVersion":%d,"reason":"MANUAL","responses":[{"questionOrder":1,"responses":["T"],"justificationText":"ok"}]}
                                """.formatted(attempt.path("version").asLong() + 1)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/teacher/research/releases").with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].publishId").value(publishId))
                .andExpect(jsonPath("$.data[0].submittedCount").value(1));

        MvcResult overview = mockMvc.perform(get("/api/teacher/research/publishes/{publishId}/overview", publishId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rates.completionRate.numerator").value(1))
                .andExpect(jsonPath("$.data.rates.completionRate.denominator").value(1))
                .andExpect(jsonPath("$.data.funnel.submitted").value(1))
                .andReturn();
        assertThat(readJson(overview).path("data").path("rates").path("completionRate").path("value").asDouble()).isEqualTo(1.0d);

        mockMvc.perform(get("/api/teacher/research/publishes/{publishId}/attempts", publishId).with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].participantCode").value(org.hamcrest.Matchers.startsWith("P-")))
                .andExpect(jsonPath("$.data.records[0].attemptId").value(attemptId));

        mockMvc.perform(get("/api/teacher/research/attempts/{attemptId}", attemptId).with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.participant.participantCode").value(org.hamcrest.Matchers.startsWith("P-")))
                .andExpect(jsonPath("$.data.attempt.attemptId").value(attemptId))
                .andExpect(jsonPath("$.data.questions[0].responses[0]").value("T"));

        mockMvc.perform(get("/api/teacher/research/attempts/{attemptId}", attemptId).with(bearer(outsiderToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/teacher/assessments/attempts/{attemptId}/result", attemptId).with(bearer(teacherToken)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void completionRateIsNullWhenNobodyStartedAndStatisticsUseValidAnswers() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long paperId = createChoicePaper(teacherToken);
        MvcResult published = publishPublic(teacherToken, paperId);
        long publishId = readJson(published).path("data").path("publishId").asLong();
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();
        String firstCode = readJson(published).path("data").path("participationCodes").get(0).asText();
        String secondCode = readJson(published).path("data").path("participationCodes").get(1).asText();

        mockMvc.perform(get("/api/teacher/research/publishes/{publishId}/overview", publishId).with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rates.completionRate.value").doesNotExist())
                .andExpect(jsonPath("$.data.rates.completionRate.denominator").value(0));

        submitChoice(releaseCode, firstCode, "A");
        Cookie second = enterByCode(releaseCode, secondCode);
        JsonNode secondAttempt = attemptData(releaseCode, second);
        mockMvc.perform(post("/api/public/assessments/{releaseCode}/responses", releaseCode)
                        .cookie(second)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseVersion":%d,"responses":[{"questionOrder":1,"responses":["B"]}]}
                                """.formatted(secondAttempt.path("version").asLong())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/teacher/research/publishes/{publishId}/overview", publishId).with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rates.completionRate.numerator").value(1))
                .andExpect(jsonPath("$.data.rates.completionRate.denominator").value(2));

        mockMvc.perform(get("/api/teacher/research/publishes/{publishId}/statistics/questions", publishId).with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.meta.sampleCount").value(1))
                .andExpect(jsonPath("$.data.questions[0].answeredCount").value(1))
                .andExpect(jsonPath("$.data.questions[0].correctRate").value(1.0));

        mockMvc.perform(post("/api/teacher/research/publishes/{publishId}/ai-reports", publishId).with(bearer(teacherToken)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attachmentsAreBoundScannedAndDownloadedWithoutObjectKeys() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        String outsiderToken = loginAndGetAccessToken("student.li", "Student@123456");
        long paperId = createFilePaper(teacherToken);
        MvcResult published = publishPublic(teacherToken, paperId);
        long publishId = readJson(published).path("data").path("publishId").asLong();
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();
        String participationCode = readJson(published).path("data").path("participationCodes").get(0).asText();
        Cookie cookie = enterByCode(releaseCode, participationCode);
        JsonNode attempt = attemptData(releaseCode, cookie);

        byte[] pdf = "%PDF-1.4 test".getBytes();
        MvcResult initiated = mockMvc.perform(post("/api/public/assessments/{releaseCode}/files/initiate", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionOrder":1,"fileName":"notes.pdf","contentType":"application/pdf","sizeBytes":%d}
                                """.formatted(pdf.length)))
                .andExpect(status().isOk())
                .andReturn();
        String token = readJson(initiated).path("data").path("uploadToken").asText();
        mockMvc.perform(multipart("/api/public/assessments/{releaseCode}/files/{uploadToken}/content", releaseCode, token)
                        .file(new MockMultipartFile("file", "notes.pdf", "application/pdf", pdf))
                        .cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scanStatus").value("CLEAN"))
                .andExpect(jsonPath("$.data.objectKey").doesNotExist());

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/files/initiate", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"questionOrder":1,"fileName":"evil.exe","contentType":"application/octet-stream","sizeBytes":4}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/responses", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseVersion":%d,"responses":[{"questionOrder":1,"attachmentTokens":["%s"]}]}
                                """.formatted(attempt.path("version").asLong(), token)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/public/assessments/{releaseCode}/submit", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseVersion":%d,"reason":"MANUAL","responses":[{"questionOrder":1,"attachmentTokens":["%s"]}]}
                                """.formatted(attempt.path("version").asLong() + 1, token)))
                .andExpect(status().isOk());

        MvcResult detail = mockMvc.perform(get("/api/teacher/research/attempts/{attemptId}", attempt.path("attemptId").asLong())
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions[0].attachments[0].originalFileName").value("notes.pdf"))
                .andExpect(jsonPath("$.data.questions[0].attachments[0].objectKey").doesNotExist())
                .andExpect(jsonPath("$.data.questions[0].attachments[0].downloadable").value(true))
                .andReturn();
        long fileId = readJson(detail).path("data").path("questions").get(0).path("attachments").get(0).path("fileId").asLong();

        mockMvc.perform(get("/api/teacher/research/files/{fileId}/download", fileId).with(bearer(outsiderToken)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/teacher/research/files/{fileId}/download", fileId).with(bearer(teacherToken)))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/public/assessments/{releaseCode}/files/{uploadToken}", releaseCode, token).cookie(cookie))
                .andExpect(status().isConflict());
    }

    @Test
    void groupReportRequiresMinimumSampleAndReusesSnapshot() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long paperId = createChoicePaper(teacherToken);
        MvcResult published = publishPublic(teacherToken, paperId);
        long publishId = readJson(published).path("data").path("publishId").asLong();
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();
        submitChoice(releaseCode, readJson(published).path("data").path("participationCodes").get(0).asText(), "A");
        submitChoice(releaseCode, readJson(published).path("data").path("participationCodes").get(1).asText(), "B");

        mockMvc.perform(patch("/api/teacher/assessments/publishes/{publishId}/public-release", publishId)
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrEntryEnabled\":true}"))
                .andExpect(status().isOk());
        for (int i = 0; i < 3; i++) {
            submitQr(releaseCode, "0123456789abcdef0123456789abcd" + i, "A");
        }

        MvcResult first = mockMvc.perform(post("/api/teacher/research/publishes/{publishId}/ai-reports", publishId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.snapshot.sampleCount").value(5))
                .andReturn();
        long reportId = readJson(first).path("data").path("reportId").asLong();
        MvcResult second = mockMvc.perform(post("/api/teacher/research/publishes/{publishId}/ai-reports", publishId)
                        .with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(readJson(second).path("data").path("reportId").asLong()).isEqualTo(reportId);

        ResearchAiReportEntity report = reportMapper.selectById(reportId);
        report.setStatus("PROCESSING");
        report.setRetryCount(2);
        reportMapper.updateById(report);
        reportProcessor.processClaimed(reportId);
        mockMvc.perform(get("/api/teacher/research/ai-reports/{reportId}", reportId).with(bearer(teacherToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FALLBACK"))
                .andExpect(jsonPath("$.data.source").value("RULE_FALLBACK"))
                .andExpect(jsonPath("$.data.ruleFallback.researchCautions[0]").exists());
    }

    private void submitChoice(String releaseCode, String participationCode, String option) throws Exception {
        Cookie cookie = enterByCode(releaseCode, participationCode);
        JsonNode attempt = attemptData(releaseCode, cookie);
        mockMvc.perform(post("/api/public/assessments/{releaseCode}/submit", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseVersion":%d,"reason":"MANUAL","responses":[{"questionOrder":1,"responses":["%s"]}]}
                                """.formatted(attempt.path("version").asLong(), option)))
                .andExpect(status().isOk());
    }

    private void submitQr(String releaseCode, String fingerprint, String option) throws Exception {
        MvcResult entered = mockMvc.perform(post("/api/public/assessments/{releaseCode}/qr-entry", releaseCode)
                        .header("X-Forwarded-For", "203.0.113.40")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"browserFingerprint\":\"%s\"}".formatted(fingerprint)))
                .andExpect(status().isOk())
                .andReturn();
        Cookie cookie = sessionCookie(entered);
        JsonNode attempt = attemptData(releaseCode, cookie);
        mockMvc.perform(post("/api/public/assessments/{releaseCode}/submit", releaseCode)
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseVersion":%d,"reason":"MANUAL","responses":[{"questionOrder":1,"responses":["%s"]}]}
                                """.formatted(attempt.path("version").asLong(), option)))
                .andExpect(status().isOk());
    }

    private Cookie enterByCode(String releaseCode, String participationCode) throws Exception {
        MvcResult verified = mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .header("X-Forwarded-For", "203.0.113.21")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participationCode\":\"%s\"}".formatted(participationCode)))
                .andExpect(status().isOk())
                .andReturn();
        return sessionCookie(verified);
    }

    private long createPaper(String teacherToken) throws Exception {
        return createPaper(teacherToken, """
                {
                  "questionType":"TRUE_FALSE_WITH_JUSTIFICATION",
                  "stemText":"The statement is correct",
                  "options":[{"key":"T","label":"True"},{"key":"F","label":"False"}],
                  "correctAnswers":["T"],
                  "explanationText":"T is correct",
                  "score":1
                }
                """);
    }

    private long createChoicePaper(String teacherToken) throws Exception {
        return createPaper(teacherToken, """
                {
                  "questionType":"SINGLE_CHOICE",
                  "stemText":"Pick the cognate",
                  "options":[{"key":"A","label":"table"},{"key":"B","label":"chair"}],
                  "correctAnswers":["A"],
                  "explanationText":"A",
                  "score":1,
                  "transferCategory":"COGNATE",
                  "constructCode":"LEXICAL_TRANSFER"
                }
                """);
    }

    private long createFilePaper(String teacherToken) throws Exception {
        return createPaper(teacherToken, """
                {
                  "questionType":"FILE_UPLOAD",
                  "stemText":"Upload your notes",
                  "correctAnswers":[],
                  "score":0,
                  "requiredAnswer":true
                }
                """);
    }

    private long createPaper(String teacherToken, String questionJson) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/assessments/papers")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Research analytics integration",
                                  "paperPurpose":"RESEARCH_SURVEY",
                                  "durationMinutes":40,
                                  "questions":[%s]
                                }
                                """.formatted(questionJson)))
                .andExpect(status().isOk())
                .andReturn();
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
                                LocalDateTime.now().plusHours(2).format(ISO_TIME))))
                .andExpect(status().isOk())
                .andReturn();
    }

    private JsonNode attemptData(String releaseCode, Cookie cookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/public/assessments/{releaseCode}/attempt", releaseCode).cookie(cookie))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).path("data");
    }

    private Cookie sessionCookie(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String value = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
        return new Cookie("LEXIBRIDGE_SESSION", value);
    }
}
