package com.huashi.eftransfer.ai.modules.internal;

import com.huashi.eftransfer.ai.common.config.InternalApiProperties;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.ai.modules.internal.controller.InternalAiConfigController;
import com.huashi.eftransfer.ai.modules.internal.service.AiConfigProbeService;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigApplyResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigIssue;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigNotice;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigStageResponse;
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

    @MockBean
    private AiConfigProbeService aiConfigProbeService;

    @Test
    void shouldReturnEffectiveConfigSnapshot() throws Exception {
        when(aiRuntimeConfigService.effective()).thenReturn(new AiOpsConfigEffectiveResponse(
                null,
                "DEFAULTS",
                1L,
                OffsetDateTime.now(),
                List.of(new AiOpsConfigNotice(
                        "automatic_failover_enabled",
                        "info",
                        "Automatic failover is enabled for retryable provider failures and circuit-open scenarios."
                ))
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
                List.of(new AiOpsConfigIssue("provider.providers.Primary OpenAI", "provider key must contain only lowercase letters, numbers, hyphen, or underscore")),
                List.of(new AiOpsConfigNotice(
                        "automatic_failover_enabled",
                        "info",
                        "Automatic failover is enabled for retryable provider failures and circuit-open scenarios."
                ))
        ));

        mockMvc.perform(post("/internal/ai/config/validate")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": {
                                    "activeProvider": "deepseek",
                                    "fallbackProvider": "deepseek",
                                    "providers": {
                                      "deepseek": {
                                        "chat": {"protocol": "openai-compat", "baseUrl": "https://example.com/v1", "apiKey": "x", "model": "m", "connectTimeout": "PT3S", "readTimeout": "PT30S", "temperature": 0.2, "maxTokens": 1024},
                                        "embedding": {"protocol": "openai-compat", "baseUrl": "https://example.com/v1", "apiKey": "x", "model": "m", "connectTimeout": "PT3S", "readTimeout": "PT30S", "dimension": 1024},
                                        "rerank": {"protocol": "qwen-rerank", "baseUrl": "https://example.com", "apiKey": "x", "model": "m", "connectTimeout": "PT3S", "readTimeout": "PT30S"}
                                      }
                                    }
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
                                    "retrieval": {"recallTopK": 20, "recallThreshold": 0.55, "rerankTopN": 8, "rerankThreshold": 0.2, "finalTopK": 6, "hnswEfSearch": 64}
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(false))
                .andExpect(jsonPath("$.data.issues[0].field").value("provider.providers.Primary OpenAI"));
    }

    @Test
    void shouldStageConfig() throws Exception {
        when(aiRuntimeConfigService.stage(any(AiOpsConfigPayload.class), any(), any())).thenReturn(new AiOpsConfigStageResponse(
                "stage-1",
                "DATABASE",
                9L,
                OffsetDateTime.now(),
                List.of()
        ));

        mockMvc.perform(post("/internal/ai/config/stage")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "config": {
                                    "provider": {
                                      "activeProvider": "deepseek",
                                      "fallbackProvider": "deepseek",
                                      "providers": {
                                        "deepseek": {
                                          "chat": {"protocol": "openai-compat", "baseUrl": "https://example.com/v1", "apiKey": "x", "model": "m", "connectTimeout": "PT3S", "readTimeout": "PT30S", "temperature": 0.2, "maxTokens": 1024},
                                          "embedding": {"protocol": "openai-compat", "baseUrl": "https://example.com/v1", "apiKey": "x", "model": "m", "connectTimeout": "PT3S", "readTimeout": "PT30S", "dimension": 1024},
                                          "rerank": {"protocol": "qwen-rerank", "baseUrl": "https://example.com", "apiKey": "x", "model": "m", "connectTimeout": "PT3S", "readTimeout": "PT30S"}
                                        }
                                      }
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
                                      "retrieval": {"recallTopK": 20, "recallThreshold": 0.55, "rerankTopN": 8, "rerankThreshold": 0.2, "finalTopK": 6, "hnswEfSearch": 64}
                                    }
                                  },
                                  "source": "DATABASE",
                                  "version": 9
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stageId").value("stage-1"))
                .andExpect(jsonPath("$.data.version").value(9));
    }

    @Test
    void shouldCommitStagedConfig() throws Exception {
        when(aiRuntimeConfigService.commit("stage-1")).thenReturn(new AiOpsConfigApplyResponse(
                "DATABASE",
                9L,
                OffsetDateTime.now(),
                List.of()
        ));

        mockMvc.perform(post("/internal/ai/config/commit")
                        .header("X-Internal-Token", "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stageId": "stage-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("DATABASE"))
                .andExpect(jsonPath("$.data.version").value(9));
    }
}
