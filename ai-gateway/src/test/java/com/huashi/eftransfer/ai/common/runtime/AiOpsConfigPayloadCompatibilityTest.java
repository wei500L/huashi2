package com.huashi.eftransfer.ai.common.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiOpsConfigPayloadCompatibilityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldDeserializeLegacySingleTimeoutIntoSplitTimeoutFields() throws Exception {
        AiOpsConfigPayload payload = objectMapper.readValue("""
                {
                  "provider": {
                    "activeProvider": "qwen",
                    "fallbackProvider": "deepseek",
                    "providers": {
                      "qwen": {
                        "chat": {
                          "protocol": "openai-compat",
                          "baseUrl": "https://example.com/v1",
                          "apiKey": "chat-key",
                          "model": "qwen-max",
                          "timeout": "PT30S",
                          "temperature": 0.2,
                          "maxTokens": 1024
                        },
                        "embedding": {
                          "protocol": "openai-compat",
                          "baseUrl": "https://example.com/v1",
                          "apiKey": "embed-key",
                          "model": "text-embedding-v4",
                          "timeout": "PT12S",
                          "dimension": 1536
                        },
                        "rerank": {
                          "protocol": "qwen-rerank",
                          "baseUrl": "https://example.com/rerank",
                          "apiKey": "rerank-key",
                          "model": "gte-rerank-v2",
                          "timeout": "PT8S"
                        }
                      }
                    }
                  },
                  "resilience": {
                    "maxAttempts": 1,
                    "backoff": "PT0.1S",
                    "failureRateThreshold": 50.0,
                    "slidingWindowSize": 10,
                    "waitDurationInOpenState": "PT5S"
                  },
                  "rag": {
                    "appServer": {
                      "baseUrl": "http://localhost:8080",
                      "internalToken": "token",
                      "connectTimeout": "PT3S",
                      "readTimeout": "PT5S"
                    },
                    "ingestion": {
                      "batchSize": 100,
                      "maxConcurrency": 32
                    },
                    "retrieval": {
                      "candidateCount": 20,
                      "minScore": 0.55,
                      "maxContextChunks": 8,
                      "lexicalWeight": 0.2,
                      "rerankTopN": 6,
                      "hnswEfSearch": 64
                    }
                  }
                }
                """, AiOpsConfigPayload.class);

        assertThat(payload.provider().providers().get("qwen").chat().connectTimeout()).isEqualTo("PT30S");
        assertThat(payload.provider().providers().get("qwen").chat().readTimeout()).isEqualTo("PT30S");
        assertThat(payload.provider().providers().get("qwen").embedding().connectTimeout()).isEqualTo("PT12S");
        assertThat(payload.provider().providers().get("qwen").embedding().readTimeout()).isEqualTo("PT12S");
        assertThat(payload.provider().providers().get("qwen").rerank().connectTimeout()).isEqualTo("PT8S");
        assertThat(payload.provider().providers().get("qwen").rerank().readTimeout()).isEqualTo("PT8S");
    }
}
