package com.huashi.eftransfer.ai.modules.internal;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.huashi.eftransfer.ai.common.config.AiProviderConfiguration;
import com.huashi.eftransfer.ai.common.config.AiProviderProperties;
import com.huashi.eftransfer.ai.common.config.AiResilienceProperties;
import com.huashi.eftransfer.ai.common.exception.GlobalExceptionHandler;
import com.huashi.eftransfer.ai.common.exception.ProviderErrorSupport;
import com.huashi.eftransfer.ai.common.filter.TraceFilter;
import com.huashi.eftransfer.ai.common.observability.AiProviderObservationService;
import com.huashi.eftransfer.ai.common.observability.ResilientAiExecutor;
import com.huashi.eftransfer.ai.integration.provider.AiProviderRegistry;
import com.huashi.eftransfer.ai.integration.provider.QwenAiProviderFacade;
import com.huashi.eftransfer.ai.integration.provider.QwenChatProviderClient;
import com.huashi.eftransfer.ai.integration.provider.QwenEmbeddingProviderClient;
import com.huashi.eftransfer.ai.integration.provider.QwenRerankClient;
import com.huashi.eftransfer.ai.modules.internal.controller.InternalAiController;
import com.huashi.eftransfer.ai.modules.internal.service.InternalAiService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalAiController.class)
@AutoConfigureMockMvc
@Import({
        GlobalExceptionHandler.class,
        TraceFilter.class,
        InternalAiService.class,
        AiProviderConfiguration.class,
        ProviderErrorSupport.class,
        AiProviderObservationService.class,
        ResilientAiExecutor.class,
        AiProviderRegistry.class,
        QwenAiProviderFacade.class,
        QwenChatProviderClient.class,
        QwenEmbeddingProviderClient.class,
        QwenRerankClient.class,
        InternalAiControllerIntegrationTest.TestBeans.class
})
class InternalAiControllerIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void beforeAll() {
        wireMockServer = new WireMockServer(options().dynamicPort());
        wireMockServer.start();
        com.github.tomakehurst.wiremock.client.WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void afterAll() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ai.provider.active-provider", () -> "qwen");
        registry.add("ai.provider.fallback-provider", () -> "deepseek");
        registry.add("ai.provider.providers.qwen.chat.base-url", () -> wireMockServer.baseUrl() + "/v1");
        registry.add("ai.provider.providers.qwen.chat.api-key", () -> "test-api-key");
        registry.add("ai.provider.providers.qwen.chat.model", () -> "qwen-max");
        registry.add("ai.provider.providers.qwen.embedding.base-url", () -> wireMockServer.baseUrl() + "/v1");
        registry.add("ai.provider.providers.qwen.embedding.api-key", () -> "test-api-key");
        registry.add("ai.provider.providers.qwen.embedding.model", () -> "text-embedding-v4");
        registry.add("ai.provider.providers.qwen.embedding.dimension", () -> "3");
        registry.add("ai.provider.providers.qwen.rerank.base-url", () -> wireMockServer.baseUrl() + "/rerank");
        registry.add("ai.provider.providers.qwen.rerank.api-key", () -> "test-api-key");
        registry.add("ai.provider.providers.qwen.rerank.model", () -> "gte-rerank-v2");
        registry.add("ai.resilience.max-attempts", () -> "1");
    }

    @Test
    void shouldReturnChatApiResponse() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "chat-integration-1",
                                  "object": "chat.completion",
                                  "created": 1710000000,
                                  "model": "qwen-max",
                                  "choices": [
                                    {
                                      "index": 0,
                                      "message": {
                                        "role": "assistant",
                                        "content": "integration hello"
                                      },
                                      "finish_reason": "stop"
                                    }
                                  ],
                                  "usage": {
                                    "prompt_tokens": 7,
                                    "completion_tokens": 3,
                                    "total_tokens": 10
                                  }
                                }
                                """)));

        mockMvc.perform(post("/internal/ai/chat")
                        .header("X-Trace-Id", "trace-chat-int")
                        .contentType("application/json")
                        .content("""
                                {
                                  "messages": [
                                    {"role": "user", "content": "Say hi"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "trace-chat-int"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.traceId").value("trace-chat-int"))
                .andExpect(jsonPath("$.data.provider").value("qwen"))
                .andExpect(jsonPath("$.data.content").value("integration hello"))
                .andExpect(jsonPath("$.data.usage.totalTokens").value(10));
    }

    @Test
    void shouldReturnStructuredChatApiResponse() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "chat-structured-1",
                                  "object": "chat.completion",
                                  "created": 1710000000,
                                  "model": "qwen-max",
                                  "choices": [
                                    {
                                      "index": 0,
                                      "message": {
                                        "role": "assistant",
                                        "content": "{\\"summary\\":\\"ok\\",\\"score\\":95}"
                                      },
                                      "finish_reason": "stop"
                                    }
                                  ],
                                  "usage": {
                                    "prompt_tokens": 11,
                                    "completion_tokens": 5,
                                    "total_tokens": 16
                                  }
                                }
                                """)));

        mockMvc.perform(post("/internal/ai/chat/structured")
                        .header("X-Trace-Id", "trace-structured-int")
                        .contentType("application/json")
                        .content("""
                                {
                                  "messages": [
                                    {"role": "user", "content": "Return structured data"}
                                  ],
                                  "schemaName": "ResultSchema",
                                  "strict": true,
                                  "schema": {
                                    "type": "object",
                                    "properties": {
                                      "summary": {"type": "string"},
                                      "score": {"type": "integer"}
                                    },
                                    "required": ["summary", "score"]
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerRequestId").value("chat-structured-1"))
                .andExpect(jsonPath("$.data.structuredData.summary").value("ok"))
                .andExpect(jsonPath("$.data.structuredData.score").value(95));
    }

    @Test
    void shouldReturnEmbeddingBatchApiResponse() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/v1/embeddings"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "object": "list",
                                  "model": "text-embedding-v4",
                                  "data": [
                                    {"object": "embedding", "index": 0, "embedding": [0.1, 0.2, 0.3]},
                                    {"object": "embedding", "index": 1, "embedding": [0.4, 0.5, 0.6]}
                                  ],
                                  "usage": {
                                    "prompt_tokens": 6,
                                    "total_tokens": 6
                                  }
                                }
                                """)));

        mockMvc.perform(post("/internal/ai/embed/batch")
                        .header("X-Trace-Id", "trace-embed-int")
                        .contentType("application/json")
                        .content("""
                                {
                                  "texts": ["alpha", "beta"],
                                  "dimension": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].text").value("alpha"))
                .andExpect(jsonPath("$.data.items[1].embedding[2]").value(0.6))
                .andExpect(jsonPath("$.data.dimension").value(3));
    }

    @Test
    void shouldReturnRerankApiResponse() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/rerank"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "request_id": "rerank-int-1",
                                  "usage": {
                                    "total_tokens": 18
                                  },
                                  "output": {
                                    "results": [
                                      {"index": 1, "relevance_score": 0.89, "document": "doc-b"},
                                      {"index": 0, "relevance_score": 0.20, "document": "doc-a"}
                                    ]
                                  }
                                }
                                """)));

        mockMvc.perform(post("/internal/ai/rerank")
                        .header("X-Trace-Id", "trace-rerank-int")
                        .contentType("application/json")
                        .content("""
                                {
                                  "query": "hello",
                                  "documents": ["doc-a", "doc-b"],
                                  "topN": 2,
                                  "returnDocuments": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.providerRequestId").value("rerank-int-1"))
                .andExpect(jsonPath("$.data.items[0].index").value(1))
                .andExpect(jsonPath("$.data.items[0].relevanceScore").value(0.89));
    }

    @Test
    void shouldRejectRerankTopNGreaterThanDocumentsSize() throws Exception {
        mockMvc.perform(post("/internal/ai/rerank")
                        .header("X-Trace-Id", "trace-rerank-bad-request")
                        .contentType("application/json")
                        .content("""
                                {
                                  "query": "hello",
                                  "documents": ["doc-a", "doc-b"],
                                  "topN": 3
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("topN must be less than or equal to documents size"));
    }

    @Test
    void shouldMapProviderRateLimitError() throws Exception {
        stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "error": {
                                    "code": "rate_limit_exceeded",
                                    "message": "too many requests"
                                  },
                                  "request_id": "rate-limit-1"
                                }
                                """)));

        mockMvc.perform(post("/internal/ai/chat")
                        .header("X-Trace-Id", "trace-rate-limit")
                        .contentType("application/json")
                        .content("""
                                {
                                  "messages": [
                                    {"role": "user", "content": "Say hi"}
                                  ]
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RATE_LIMITED"))
                .andExpect(jsonPath("$.message").value("too many requests"));
    }

    @TestConfiguration
    static class TestBeans {

        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }
}
