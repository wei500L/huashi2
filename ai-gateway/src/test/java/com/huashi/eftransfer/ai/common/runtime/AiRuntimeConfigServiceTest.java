package com.huashi.eftransfer.ai.common.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.common.config.AiProviderProperties;
import com.huashi.eftransfer.ai.common.config.AiResilienceProperties;
import com.huashi.eftransfer.ai.common.exception.ProviderErrorSupport;
import com.huashi.eftransfer.ai.common.observability.ResilientAiExecutor;
import com.huashi.eftransfer.ai.modules.rag.config.RagProperties;
import com.huashi.eftransfer.shared.ai.config.AiOpsChatConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigIssue;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsEmbeddingConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsProviderConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagAppServerConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagIngestionConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagRetrievalConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRerankConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsResilienceConfig;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.security.InternalApiHeaders;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AiRuntimeConfigServiceTest {

    private static HttpServer server;
    private static String baseUrl;
    private static final AtomicReference<String> lastInternalToken = new AtomicReference<>();

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeAll
    static void beforeAll() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/ops/ai-config", exchange -> {
            lastInternalToken.set(exchange.getRequestHeaders().getFirst(InternalApiHeaders.INTERNAL_TOKEN));
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldRejectBlankAppServerInternalTokenDuringValidation() {
        AiRuntimeConfigService service = runtimeConfigService(baseUrl, "test-internal-token");

        var validation = service.validate(payload(baseUrl, ""));

        assertThat(validation.valid()).isFalse();
        assertThat(validation.issues())
                .extracting(AiOpsConfigIssue::field)
                .contains("rag.appServer.internalToken");
    }

    @Test
    void shouldSyncStoredConfigWithInternalTokenHeader() throws Exception {
        lastInternalToken.set(null);
        AiOpsConfigPayload payload = payload(baseUrl, "test-internal-token");
        server.removeContext("/internal/ops/ai-config");
        server.createContext("/internal/ops/ai-config", exchange -> {
            lastInternalToken.set(exchange.getRequestHeaders().getFirst(InternalApiHeaders.INTERNAL_TOKEN));
            AiOpsConfigEffectiveResponse effective = new AiOpsConfigEffectiveResponse(
                    payload,
                    "APP_SERVER_SYNC",
                    2L,
                    OffsetDateTime.parse("2026-03-21T00:00:00Z"),
                    List.of()
            );
            byte[] body = objectMapper.writeValueAsBytes(ApiResponse.success(effective, "trace-ai-runtime-sync"));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        AiRuntimeConfigService service = runtimeConfigService(baseUrl, "test-internal-token");
        service.syncStoredConfigAfterStartup();

        assertThat(lastInternalToken.get()).isEqualTo("test-internal-token");
        assertThat(service.current().version()).isEqualTo(2L);
    }

    private AiRuntimeConfigService runtimeConfigService(String appServerBaseUrl, String internalToken) {
        AiProviderProperties providerProperties = new AiProviderProperties();
        providerProperties.setActiveProvider("qwen");
        providerProperties.setFallbackProvider("deepseek");
        AiProviderProperties.ProviderProperties qwen = new AiProviderProperties.ProviderProperties();
        qwen.getChat().setBaseUrl("https://example.com/v1");
        qwen.getChat().setApiKey("chat-key");
        qwen.getChat().setModel("qwen-max");
        qwen.getEmbedding().setBaseUrl("https://example.com/v1");
        qwen.getEmbedding().setApiKey("embed-key");
        qwen.getEmbedding().setModel("text-embedding-v4");
        qwen.getEmbedding().setDimension(1024);
        qwen.getRerank().setBaseUrl("https://example.com");
        qwen.getRerank().setApiKey("rerank-key");
        qwen.getRerank().setModel("gte-rerank-v2");
        providerProperties.getProviders().put("qwen", qwen);

        AiResilienceProperties resilienceProperties = new AiResilienceProperties();
        RagProperties ragProperties = new RagProperties();
        ragProperties.getAppServer().setBaseUrl(appServerBaseUrl);
        ragProperties.getAppServer().setInternalToken(internalToken);

        AiRuntimeBundleFactory bundleFactory = new AiRuntimeBundleFactory(
                RestClient.builder(),
                (request, body, execution) -> execution.execute(request, body),
                new ProviderErrorSupport(objectMapper)
        );
        ResilientAiExecutor resilientAiExecutor = new ResilientAiExecutor(
                RetryRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults()
        );
        AiRuntimeConfigService service = new AiRuntimeConfigService(
                providerProperties,
                resilienceProperties,
                ragProperties,
                bundleFactory,
                resilientAiExecutor
        );
        ReflectionTestUtils.invokeMethod(service, "initialize");
        return service;
    }

    private AiOpsConfigPayload payload(String appServerBaseUrl, String internalToken) {
        return new AiOpsConfigPayload(
                new AiOpsProviderConfig(
                        "qwen",
                        "deepseek",
                        new AiOpsChatConfig("https://example.com/v1", "chat-key", "qwen-max", "PT30S", 0.2d, 1024),
                        new AiOpsEmbeddingConfig("https://example.com/v1", "embed-key", "text-embedding-v4", "PT30S", 1024),
                        new AiOpsRerankConfig("https://example.com", "rerank-key", "gte-rerank-v2", "PT30S")
                ),
                new AiOpsResilienceConfig(3, "PT0.5S", 50.0f, 20, "PT30S"),
                new AiOpsRagConfig(
                        new AiOpsRagAppServerConfig(appServerBaseUrl, internalToken, "PT3S", "PT5S"),
                        new AiOpsRagIngestionConfig(100, 32),
                        new AiOpsRagRetrievalConfig(20, 0.55d, 8, 0.2d, 6)
                )
        );
    }
}
