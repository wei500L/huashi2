package com.huashi.eftransfer.ai.modules.internal;

import com.huashi.eftransfer.ai.common.config.InternalApiProperties;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.ai.modules.internal.controller.InternalAiConfigController;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigIssue;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigValidationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalAiConfigController.class)
@EnableConfigurationProperties(InternalApiProperties.class)
@TestPropertySource(properties = {
        "platform.internal-api.enabled=true",
        "platform.internal-api.token=test-internal-token"
})
class InternalAiConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiRuntimeConfigService aiRuntimeConfigService;

    @Test
    void shouldReturnEffectiveConfigSnapshot() throws Exception {
        when(aiRuntimeConfigService.effective()).thenReturn(new AiOpsConfigEffectiveResponse(
                null,
                "DEFAULTS",
                1L,
                OffsetDateTime.now(),
                List.of("fallbackProvider is currently informational only; automatic failover is not implemented.")
        ));

        mockMvc.perform(get("/internal/ai/config/effective")
                        .header("X-Internal-Token", "test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("DEFAULTS"))
                .andExpect(jsonPath("$.data.version").value(1));
    }

    @Test
    void shouldReturnValidationIssues() throws Exception {
        when(aiRuntimeConfigService.validate(any(AiOpsConfigPayload.class))).thenReturn(new AiOpsConfigValidationResponse(
                false,
                List.of(new AiOpsConfigIssue("provider.activeProvider", "Only qwen is currently implemented as active provider")),
                List.of("fallbackProvider is currently informational only; automatic failover is not implemented.")
        ));

        mockMvc.perform(post("/internal/ai/config/validate")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": {
                                    "activeProvider": "deepseek",
                                    "fallbackProvider": "deepseek",
                                    "chat": {"baseUrl": "https://example.com/v1", "apiKey": "x", "model": "m", "timeout": "PT30S", "temperature": 0.2, "maxTokens": 1024},
                                    "embedding": {"baseUrl": "https://example.com/v1", "apiKey": "x", "model": "m", "timeout": "PT30S", "dimension": 1024},
                                    "rerank": {"baseUrl": "https://example.com", "apiKey": "x", "model": "m", "timeout": "PT30S"}
                                  },
                                  "resilience": {
                                    "maxAttempts": 3,
                                    "waitDuration": "PT0.5S",
                                    "failureRateThreshold": 50,
                                    "slidingWindowSize": 20,
                                    "openStateDuration": "PT30S"
                                  },
                                  "rag": {
                                    "appServer": {"baseUrl": "http://localhost:8080", "internalToken": "t", "connectTimeout": "PT3S", "readTimeout": "PT5S"},
                                    "ingestion": {"exportPageSize": 100, "embeddingBatchSize": 32},
                                    "retrieval": {"recallTopK": 20, "recallThreshold": 0.55, "rerankTopN": 8, "rerankThreshold": 0.2, "finalTopK": 6}
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.issues[0].field").value("provider.activeProvider"));
    }
}
