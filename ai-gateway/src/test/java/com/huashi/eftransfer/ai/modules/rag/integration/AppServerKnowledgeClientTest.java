package com.huashi.eftransfer.ai.modules.rag.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeExportPageResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsChatConfig;
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
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppServerKnowledgeClientTest {

    private static HttpServer server;
    private static String baseUrl;
    private static final AtomicReference<String> lastInternalToken = new AtomicReference<>();
    private static final AtomicReference<String> lastPath = new AtomicReference<>();

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @BeforeAll
    static void beforeAll() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/internal/knowledge/lexical-pairs/export", exchange -> {
            lastInternalToken.set(exchange.getRequestHeaders().getFirst(InternalApiHeaders.INTERNAL_TOKEN));
            lastPath.set(exchange.getRequestURI().toString());
            byte[] body = new ObjectMapper().findAndRegisterModules().writeValueAsBytes(ApiResponse.success(
                    new LexicalKnowledgeExportPageResponse(List.of(), null, null),
                    "trace-knowledge-export"
            ));
            exchange.getResponseHeaders().add("Content-Type", "application/json");
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
    void shouldExportLexicalPairsWithInternalTokenHeader() {
        lastInternalToken.set(null);
        lastPath.set(null);

        AiRuntimeConfigService runtimeConfigService = mock(AiRuntimeConfigService.class);
        when(runtimeConfigService.current()).thenReturn(runtimeBundle());

        AppServerKnowledgeClient client = new AppServerKnowledgeClient(runtimeConfigService);
        client.exportLexicalPairs(
                OffsetDateTime.parse("2026-03-21T00:00:00Z"),
                "cursor-1",
                20,
                List.of("1001", "1002")
        );

        assertThat(lastInternalToken.get()).isEqualTo("test-internal-token");
        var uriComponents = UriComponentsBuilder.fromUriString("http://localhost" + lastPath.get()).build();
        assertThat(uriComponents.getPath()).isEqualTo("/internal/knowledge/lexical-pairs/export");
        assertThat(uriComponents.getQueryParams().getFirst("updatedSince")).isEqualTo("2026-03-21T00:00Z");
        assertThat(uriComponents.getQueryParams().getFirst("cursor")).isEqualTo("cursor-1");
        assertThat(uriComponents.getQueryParams().getFirst("limit")).isEqualTo("20");
        assertThat(uriComponents.getQueryParams().get("ids")).containsExactly("1001", "1002");
    }

    private AiRuntimeBundle runtimeBundle() {
        return new AiRuntimeBundle(
                new AiOpsConfigPayload(
                        new AiOpsProviderConfig(
                                "qwen",
                                "deepseek",
                                new AiOpsChatConfig("https://example.com/v1", "chat-key", "qwen-max", "PT30S", 0.2d, 1024),
                                new AiOpsEmbeddingConfig("https://example.com/v1", "embed-key", "text-embedding-v4", "PT30S", 1024),
                                new AiOpsRerankConfig("https://example.com", "rerank-key", "gte-rerank-v2", "PT30S")
                        ),
                        new AiOpsResilienceConfig(3, "PT0.5S", 50.0f, 20, "PT30S"),
                        new AiOpsRagConfig(
                                new AiOpsRagAppServerConfig(baseUrl, "test-internal-token", "PT3S", "PT5S"),
                                new AiOpsRagIngestionConfig(100, 32),
                                new AiOpsRagRetrievalConfig(20, 0.55d, 8, 0.2d, 6)
                        )
                ),
                null,
                null,
                null,
                null,
                RestClient.builder()
                        .baseUrl(baseUrl)
                        .defaultHeader(InternalApiHeaders.INTERNAL_TOKEN, "test-internal-token")
                        .build(),
                RetryRegistry.ofDefaults(),
                CircuitBreakerRegistry.ofDefaults(),
                "TEST",
                1L,
                OffsetDateTime.now()
        );
    }
}
