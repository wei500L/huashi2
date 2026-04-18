package com.huashi.eftransfer.ai.integration.provider;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.huashi.eftransfer.ai.common.config.AiProviderConfiguration;
import com.huashi.eftransfer.ai.common.config.AiProviderProperties;
import com.huashi.eftransfer.ai.common.config.AiResilienceProperties;
import com.huashi.eftransfer.ai.common.exception.ProviderErrorSupport;
import com.huashi.eftransfer.ai.common.observability.AiProviderObservationService;
import com.huashi.eftransfer.ai.common.observability.ProviderRequestContextHolder;
import com.huashi.eftransfer.ai.common.observability.ResilientAiExecutor;
import com.huashi.eftransfer.ai.common.observability.SensitiveDataRedactor;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundleFactory;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.ai.modules.rag.config.RagProperties;
import com.huashi.eftransfer.shared.ai.ChatMessage;
import com.huashi.eftransfer.shared.ai.ChatRequest;
import com.huashi.eftransfer.shared.ai.ChatResponse;
import com.huashi.eftransfer.shared.ai.EmbeddingBatchRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QwenProviderClientTest {

    private static WireMockServer wireMockServer;

    private QwenChatProviderClient chatProviderClient;
    private QwenEmbeddingProviderClient embeddingProviderClient;

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
    void setUp() {
        wireMockServer.resetAll();

        AiProviderConfiguration configuration = new AiProviderConfiguration();
        AiProviderProperties properties = buildProperties();
        RagProperties ragProperties = buildRagProperties();
        ProviderRequestContextHolder requestContextHolder = configuration.providerRequestContextHolder();
        ClientHttpRequestInterceptor interceptor = configuration.providerRequestCaptureInterceptor(requestContextHolder);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ProviderErrorSupport providerErrorSupport = new ProviderErrorSupport(objectMapper, new SensitiveDataRedactor());
        AiResilienceProperties resilienceProperties = new AiResilienceProperties();
        resilienceProperties.setMaxAttempts(1);
        ResilientAiExecutor resilientAiExecutor = new ResilientAiExecutor();
        AiProviderObservationService observationService = new AiProviderObservationService(
                new SimpleMeterRegistry(),
                providerErrorSupport,
                requestContextHolder
        );
        AiRuntimeBundleFactory bundleFactory = new AiRuntimeBundleFactory(RestClient.builder(), interceptor, providerErrorSupport);
        AiRuntimeBundle runtimeBundle = bundleFactory.fromProperties(
                properties,
                resilienceProperties,
                ragProperties,
                "TEST",
                1L
        );
        AiRuntimeConfigService runtimeConfigService = mock(AiRuntimeConfigService.class);
        when(runtimeConfigService.current()).thenReturn(runtimeBundle);

        chatProviderClient = new QwenChatProviderClient(
                runtimeConfigService,
                objectMapper,
                resilientAiExecutor,
                observationService,
                requestContextHolder
        );
        embeddingProviderClient = new QwenEmbeddingProviderClient(
                runtimeConfigService,
                resilientAiExecutor,
                observationService,
                requestContextHolder
        );
    }

    @Test
    void shouldCallChatProviderAndMapResponse() {
        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withHeader("x-request-id", "provider-chat-header")
                        .withBody("""
                                {
                                  "id": "chatcmpl-123",
                                  "object": "chat.completion",
                                  "created": 1710000000,
                                  "model": "qwen-max",
                                  "choices": [
                                    {
                                      "index": 0,
                                      "message": {
                                        "role": "assistant",
                                        "content": "Hello from Qwen"
                                      },
                                      "finish_reason": "stop"
                                    }
                                  ],
                                  "usage": {
                                    "prompt_tokens": 12,
                                    "completion_tokens": 8,
                                    "total_tokens": 20
                                  }
                                }
                                """)));

        ChatResponse response = chatProviderClient.chat("qwen", new ChatRequest(
                List.of(new ChatMessage("user", "Say hello")),
                null,
                null,
                null
        ));

        assertThat(response.provider()).isEqualTo("qwen");
        assertThat(response.model()).isEqualTo("qwen-max");
        assertThat(response.content()).isEqualTo("Hello from Qwen");
        assertThat(response.finishReason()).isEqualTo("stop");
        assertThat(response.providerRequestId()).isEqualTo("chatcmpl-123");
        assertThat(response.usage().totalTokens()).isEqualTo(20);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer test-api-key"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("qwen-max"))));
    }

    @Test
    void shouldCallEmbeddingProviderAndMapBatchResponse() {
        stubFor(post(urlEqualTo("/v1/embeddings"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withHeader("x-request-id", "provider-embedding-header")
                        .withBody("""
                                {
                                  "object": "list",
                                  "model": "text-embedding-v4",
                                  "data": [
                                    {
                                      "object": "embedding",
                                      "index": 0,
                                      "embedding": [0.1, 0.2, 0.3]
                                    },
                                    {
                                      "object": "embedding",
                                      "index": 1,
                                      "embedding": [0.4, 0.5, 0.6]
                                    }
                                  ],
                                  "usage": {
                                    "prompt_tokens": 9,
                                    "total_tokens": 9
                                  }
                                }
                                """)));

        EmbeddingResponse response = embeddingProviderClient.embedBatch("qwen", new EmbeddingBatchRequest(
                List.of("alpha", "beta"),
                null,
                3
        ));

        assertThat(response.provider()).isEqualTo("qwen");
        assertThat(response.model()).isEqualTo("text-embedding-v4");
        assertThat(response.dimension()).isEqualTo(3);
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).text()).isEqualTo("alpha");
        assertThat(response.items().get(0).embedding()).containsExactly(0.1D, 0.2D, 0.3D);
        assertThat(response.usage().totalTokens()).isEqualTo(9);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/v1/embeddings"))
                .withRequestBody(equalToJson("""
                        {
                          "model": "text-embedding-v4",
                          "input": ["alpha", "beta"],
                          "dimensions": 3
                        }
                        """, true, true)));
    }

    private AiProviderProperties buildProperties() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setActiveProvider("qwen");

        AiProviderProperties.ChatProperties chatProperties = new AiProviderProperties.ChatProperties();
        chatProperties.setBaseUrl(wireMockServer.baseUrl() + "/v1");
        chatProperties.setApiKey("test-api-key");
        chatProperties.setModel("qwen-max");

        AiProviderProperties.EmbeddingProperties embeddingProperties = new AiProviderProperties.EmbeddingProperties();
        embeddingProperties.setBaseUrl(wireMockServer.baseUrl() + "/v1");
        embeddingProperties.setApiKey("test-api-key");
        embeddingProperties.setModel("text-embedding-v4");
        embeddingProperties.setDimension(1024);

        AiProviderProperties.RerankProperties rerankProperties = new AiProviderProperties.RerankProperties();
        rerankProperties.setBaseUrl(wireMockServer.baseUrl() + "/rerank");
        rerankProperties.setApiKey("test-api-key");
        rerankProperties.setModel("gte-rerank-v2");

        AiProviderProperties.ProviderProperties providerProperties = new AiProviderProperties.ProviderProperties();
        providerProperties.setChat(chatProperties);
        providerProperties.setEmbedding(embeddingProperties);
        providerProperties.setRerank(rerankProperties);

        Map<String, AiProviderProperties.ProviderProperties> providers = new LinkedHashMap<>();
        providers.put("qwen", providerProperties);
        properties.setProviders(providers);
        return properties;
    }

    private RagProperties buildRagProperties() {
        RagProperties ragProperties = new RagProperties();
        ragProperties.getAppServer().setBaseUrl("http://localhost:8080");
        ragProperties.getAppServer().setInternalToken("test-internal-token");
        return ragProperties;
    }
}
