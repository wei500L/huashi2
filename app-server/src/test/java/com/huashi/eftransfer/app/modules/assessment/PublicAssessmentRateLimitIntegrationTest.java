package com.huashi.eftransfer.app.modules.assessment;

import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.security.rate-limit.assessment.verify.ip.limit=2",
        "app.security.rate-limit.assessment.verify.ip.window=PT1H",
        "app.security.rate-limit.assessment.qr-entry.ip.limit=2",
        "app.security.rate-limit.assessment.qr-entry.ip.window=PT1H"
})
class PublicAssessmentRateLimitIntegrationTest extends AbstractWebIntegrationTest {

    private static final DateTimeFormatter ISO_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Test
    void shouldRateLimitVerifyAndKeepQrEntryOnASeparateBucket() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");
        long paperId = createPaper(teacherToken);
        MvcResult published = publishPublic(teacherToken, paperId);
        long publishId = readJson(published).path("data").path("publishId").asLong();
        String releaseCode = readJson(published).path("data").path("releaseCode").asText();

        RequestPostProcessor verifyIp = remoteAddress("203.0.113.80");
        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                            .with(verifyIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"participationCode\":\"AAAA-BBBB-CCCC\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.code").value("PARTICIPATION_CODE_INVALID"));
        }
        mockMvc.perform(post("/api/public/assessments/{releaseCode}/verify", releaseCode)
                        .with(verifyIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participationCode\":\"AAAA-BBBB-CCCC\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));

        mockMvc.perform(patch("/api/teacher/assessments/publishes/{publishId}/public-release", publishId)
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrEntryEnabled\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/public/assessments/{releaseCode}/qr-entry", releaseCode)
                        .with(verifyIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"browserFingerprint\":\"0123456789abcdef0123456789abcdef\"}"))
                .andExpect(status().isOk());
    }

    private long createPaper(String teacherToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/teacher/assessments/papers")
                        .with(bearer(teacherToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"Public research rate limit",
                                  "paperPurpose":"RESEARCH_SURVEY",
                                  "durationMinutes":40,
                                  "questions":[{
                                    "questionType":"TRUE_FALSE_WITH_JUSTIFICATION",
                                    "stemText":"The statement is correct",
                                    "options":[{"key":"T","label":"True"},{"key":"F","label":"False"}],
                                    "correctAnswers":["T"],
                                    "explanationText":"T is correct",
                                    "score":1
                                  }]
                                }
                                """))
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

    private RequestPostProcessor remoteAddress(String remoteAddress) {
        return request -> {
            request.setRemoteAddr(remoteAddress);
            return request;
        };
    }
}
