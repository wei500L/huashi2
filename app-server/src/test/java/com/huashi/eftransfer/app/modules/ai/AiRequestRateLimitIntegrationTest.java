package com.huashi.eftransfer.app.modules.ai;

import com.huashi.eftransfer.app.common.config.AiGatewayClientProperties;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "app.security.rate-limit.ai.user.limit=2",
        "app.security.rate-limit.ai.user.window=PT1H",
        "app.security.rate-limit.ai.ip.limit=100",
        "app.security.rate-limit.ai.ip.window=PT1H"
})
class AiRequestRateLimitIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private AiGatewayClientProperties aiGatewayClientProperties;

    @Test
    void aiGatewayReadTimeoutDoesNotExceedNginxBudget() {
        assertThat(aiGatewayClientProperties.getReadTimeout()).isLessThanOrEqualTo(Duration.ofSeconds(180));
    }

    @Test
    void shouldRateLimitRepeatedAiPostsButNotJobPolling() throws Exception {
        String token = loginAndGetAccessToken("student.li", "Student@123456");
        RequestPostProcessor student = bearer(token);
        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(post("/api/ai/practice-question-tutor")
                            .with(student)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/api/ai/practice-question-tutor")
                        .with(student)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"));

        mockMvc.perform(get("/api/ai/jobs/{jobId}", "missing-job").with(student))
                .andExpect(status().isNotFound());
    }
}
