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
import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;
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
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
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
        AiProviderProperties properties = buildProperties();
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
                resilientAiExecutor,
                new AiProviderObservationService(new SimpleMeterRegistry(), providerErrorSupport, contextHolder),
                contextHolder
        );
    }

    @Test
    void shouldCallGteRerankProtocol() {
        stubFor(post(urlEqualTo("/rerank"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "request_id": "rerank-req-1",
                                  "usage": {
                                    "total_tokens": 21
                                  },
                                  "output": {
                                    "results": [
                                      {"index": 1, "relevance_score": 0.91, "document": "doc-b"},
                                      {"index": 0, "relevance_score": 0.35, "document": "doc-a"}
                                    ]
                                  }
                                }
                                """)));

        RerankResponse response = rerankClient.rerank("qwen", new RerankRequest(
                null,
                "hello",
                List.of("doc-a", "doc-b"),
                2,
                true,
                null
        ));

        assertThat(response.providerRequestId()).isEqualTo("rerank-req-1");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).index()).isEqualTo(1);
        assertThat(response.items().get(0).relevanceScore()).isEqualTo(0.91D);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/rerank"))
                .withRequestBody(matchingJsonPath("$.input.query", equalTo("hello")))
                .withRequestBody(matchingJsonPath("$.parameters.top_n", equalTo("2")))
                .withRequestBody(matchingJsonPath("$.parameters.return_documents", equalTo("true"))));
    }

    @Test
    void shouldCallQwen3RerankProtocol() {
        stubFor(post(urlEqualTo("/rerank"))
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
                "teacher mode"
        ));

        assertThat(response.providerRequestId()).isEqualTo("rerank-req-2");
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).document()).isNull();

        wireMockServer.verify(postRequestedFor(urlEqualTo("/rerank"))
                .withRequestBody(matchingJsonPath("$.query", equalTo("hello")))
                .withRequestBody(matchingJsonPath("$.top_n", equalTo("1")))
                .withRequestBody(matchingJsonPath("$.instruct", equalTo("teacher mode"))));
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
