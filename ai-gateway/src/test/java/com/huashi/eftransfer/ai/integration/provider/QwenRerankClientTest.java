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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.modules.rag.config.RagProperties;
import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsProtocols;
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
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QwenRerankClientTest {

    private static WireMockServer wireMockServer;

    private QwenRerankClient rerankClient;

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
        AiProviderProperties properties = buildProperties(AiOpsProtocols.OPENAI_RERANK);
        RagProperties ragProperties = buildRagProperties();
        ProviderRequestContextHolder contextHolder = configuration.providerRequestContextHolder();
        ClientHttpRequestInterceptor interceptor = configuration.providerRequestCaptureInterceptor(contextHolder);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ProviderErrorSupport providerErrorSupport = new ProviderErrorSupport(objectMapper, new SensitiveDataRedactor());
        AiResilienceProperties resilienceProperties = new AiResilienceProperties();
        resilienceProperties.setMaxAttempts(1);
        ResilientAiExecutor resilientAiExecutor = new ResilientAiExecutor();
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
        rerankClient = new QwenRerankClient(
                runtimeConfigService,
                objectMapper,
                resilientAiExecutor,
                new AiProviderObservationService(new SimpleMeterRegistry(), providerErrorSupport, contextHolder),
                contextHolder
        );
    }

    @Test
    void shouldCallOpenAiCompatibleRerankProtocol() {
        stubFor(post(urlEqualTo("/v1/rerank"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "rerank-req-1",
                                  "usage": {
                                    "total_tokens": 21
                                  },
                                  "results": [
                                    {"index": 1, "relevance_score": 0.91, "document": {"text": "doc-b"}},
                                    {"index": 0, "relevance_score": 0.35, "document": {"text": "doc-a"}}
                                  ]
                                }
                                """)));

        RerankResponse response = rerankClient.rerank("qwen", new RerankRequest(
                null,
                "hello",
                List.of("doc-a", "doc-b"),
                2,
                true,
                null,
                null
        ));

        assertThat(response.providerRequestId()).isEqualTo("rerank-req-1");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).index()).isEqualTo(1);
        assertThat(response.items().get(0).relevanceScore()).isEqualTo(0.91D);
        assertThat(response.items().get(0).document()).isEqualTo("doc-b");

        wireMockServer.verify(postRequestedFor(urlEqualTo("/v1/rerank"))
                .withRequestBody(matchingJsonPath("$.query", equalTo("hello")))
                .withRequestBody(matchingJsonPath("$.top_n", equalTo("2")))
                .withRequestBody(matchingJsonPath("$.return_documents", equalTo("true"))));
    }

    @Test
    void shouldSendInstructionWhenPresent() {
        stubFor(post(urlEqualTo("/v1/rerank"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "request_id": "rerank-req-2",
                                  "results": [
                                    {"index": 0, "relevance_score": 0.99},
                                    {"index": 1, "relevance_score": 0.22}
                                  ]
                                }
                                """)));

        RerankResponse response = rerankClient.rerank("qwen", new RerankRequest(
                "qwen3-rerank-plus",
                "hello",
                List.of("doc-a", "doc-b"),
                1,
                false,
                null,
                "teacher mode"
        ));

        assertThat(response.providerRequestId()).isEqualTo("rerank-req-2");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).document()).isNull();

        wireMockServer.verify(postRequestedFor(urlEqualTo("/v1/rerank"))
                .withRequestBody(matchingJsonPath("$.query", equalTo("hello")))
                .withRequestBody(matchingJsonPath("$.top_n", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.instruction", equalTo("teacher mode"))));
    }

    @Test
    void shouldCallOpenAiChatCompletionsRerankProtocol() {
        rerankClient = buildClient(AiOpsProtocols.OPENAI_CHAT_RERANK);
        stubFor(post(urlEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "chat-rerank-req-1",
                                  "choices": [
                                    {
                                      "message": {
                                        "content": "{\\\"results\\\":[{\\\"index\\\":1,\\\"relevance_score\\\":0.88},{\\\"index\\\":0,\\\"relevance_score\\\":0.31}]}"
                                      }
                                    }
                                  ],
                                  "usage": {
                                    "total_tokens": 33
                                  }
                                }
                                """)));

        RerankResponse response = rerankClient.rerank("qwen", new RerankRequest(
                null,
                "hello",
                List.of("doc-a", "doc-b"),
                2,
                true,
                null,
                "teacher mode"
        ));

        assertThat(response.providerRequestId()).isEqualTo("chat-rerank-req-1");
        assertThat(response.totalTokens()).isEqualTo(33);
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).index()).isEqualTo(1);
        assertThat(response.items().get(0).document()).isEqualTo("doc-b");

        wireMockServer.verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("gte-rerank-v2")))
                .withRequestBody(matchingJsonPath("$.messages[1].content", containing("teacher mode")))
                .withRequestBody(matchingJsonPath("$.response_format.type", equalTo("json_object"))));
    }

    @Test
    void shouldRejectMultimodalRerankUntilRequestContractSupportsMedia() {
        assertThatThrownBy(() -> rerankClient.rerank("qwen", new RerankRequest(
                null,
                "hello",
                List.of("image-plus-text"),
                1,
                true,
                "multimodal",
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("text-only request contract");
    }

    private QwenRerankClient buildClient(String rerankProtocol) {
        AiProviderConfiguration configuration = new AiProviderConfiguration();
        ProviderRequestContextHolder contextHolder = configuration.providerRequestContextHolder();
        ClientHttpRequestInterceptor interceptor = configuration.providerRequestCaptureInterceptor(contextHolder);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ProviderErrorSupport providerErrorSupport = new ProviderErrorSupport(objectMapper, new SensitiveDataRedactor());
        AiResilienceProperties resilienceProperties = new AiResilienceProperties();
        resilienceProperties.setMaxAttempts(1);
        AiRuntimeBundleFactory bundleFactory = new AiRuntimeBundleFactory(RestClient.builder(), interceptor, providerErrorSupport);
        AiRuntimeBundle runtimeBundle = bundleFactory.fromProperties(
                buildProperties(rerankProtocol),
                resilienceProperties,
                buildRagProperties(),
                "TEST",
                1L
        );
        AiRuntimeConfigService runtimeConfigService = mock(AiRuntimeConfigService.class);
        when(runtimeConfigService.current()).thenReturn(runtimeBundle);
        return new QwenRerankClient(
                runtimeConfigService,
                objectMapper,
                new ResilientAiExecutor(),
                new AiProviderObservationService(new SimpleMeterRegistry(), providerErrorSupport, contextHolder),
                contextHolder
        );
    }

    private AiProviderProperties buildProperties(String rerankProtocol) {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setActiveProvider("qwen");
        properties.setFallbackProvider("backup");

        AiProviderProperties.ChatProperties chatProperties = new AiProviderProperties.ChatProperties();
        chatProperties.setBaseUrl(wireMockServer.baseUrl() + "/v1");
        chatProperties.setApiKey("test-api-key");
        chatProperties.setModel("qwen-max");

        AiProviderProperties.EmbeddingProperties embeddingProperties = new AiProviderProperties.EmbeddingProperties();
        embeddingProperties.setBaseUrl(wireMockServer.baseUrl() + "/v1");
        embeddingProperties.setApiKey("test-api-key");
        embeddingProperties.setModel("text-embedding-v4");

        AiProviderProperties.RerankProperties rerankProperties = new AiProviderProperties.RerankProperties();
        rerankProperties.setProtocol(rerankProtocol);
        rerankProperties.setBaseUrl(wireMockServer.baseUrl() + "/v1");
        rerankProperties.setApiKey("test-api-key");
        rerankProperties.setModel("gte-rerank-v2");
        rerankProperties.setMultimodalModel("Qwen/Qwen3-VL-Reranker-8B");

        AiProviderProperties.ProviderProperties providerProperties = new AiProviderProperties.ProviderProperties();
        providerProperties.setChat(chatProperties);
        providerProperties.setEmbedding(embeddingProperties);
        providerProperties.setRerank(rerankProperties);

        Map<String, AiProviderProperties.ProviderProperties> providers = new LinkedHashMap<>();
        providers.put("qwen", providerProperties);
        providers.put("backup", providerProperties);
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
