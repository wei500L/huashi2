package com.huashi.eftransfer.ai.common.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.common.config.AiProviderProperties;
import com.huashi.eftransfer.ai.common.config.AiResilienceProperties;
import com.huashi.eftransfer.ai.common.exception.ProviderErrorSupport;
import com.huashi.eftransfer.ai.common.observability.SensitiveDataRedactor;
import com.huashi.eftransfer.ai.modules.rag.config.RagProperties;
import com.huashi.eftransfer.ai.modules.rag.service.RagSchemaDimensionGuard;
import com.huashi.eftransfer.shared.ai.config.AiOpsChatConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigIssue;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsEmbeddingConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsProviderConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsProviderDefinition;
import com.huashi.eftransfer.shared.ai.config.AiOpsProtocols;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagAppServerConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagIngestionConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagRetrievalConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRerankConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsResilienceConfig;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.security.InternalApiHeaders;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

class AiRuntimeConfigServiceTest {

    private static HttpServer server;
    private static String baseUrl;
    private static final AtomicReference<String> lastInternalToken = new AtomicReference<>();
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

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
        assertThat(service.storedSyncStatus()).isEqualTo(AiRuntimeConfigService.STORED_SYNC_STATUS_IN_SYNC);
    }

    @Test
    void shouldUseStoredProviderDefinitionsAfterSyncInsteadOfBootstrapDefaults() throws Exception {
        lastInternalToken.set(null);
        AiOpsConfigPayload payload = payload(
                baseUrl,
                "test-internal-token",
                "https://stored-chat.example.com/v1",
                "stored-chat-model",
                "https://stored-embedding.example.com/v1",
                "stored-embedding-model",
                "https://stored-rerank.example.com/v1",
                "stored-rerank-model"
        );
        server.removeContext("/internal/ops/ai-config");
        server.createContext("/internal/ops/ai-config", exchange -> {
            lastInternalToken.set(exchange.getRequestHeaders().getFirst(InternalApiHeaders.INTERNAL_TOKEN));
            AiOpsConfigEffectiveResponse effective = new AiOpsConfigEffectiveResponse(
                    payload,
                    "APP_SERVER_SYNC",
                    9L,
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

        AiProviderRuntime providerRuntime = service.current().providerRuntime("qwen");
        assertThat(lastInternalToken.get()).isEqualTo("test-internal-token");
        assertThat(service.current().source()).isEqualTo("APP_SERVER_SYNC");
        assertThat(service.current().version()).isEqualTo(9L);
        assertThat(providerRuntime.definition().chat().baseUrl()).isEqualTo("https://stored-chat.example.com/v1");
        assertThat(providerRuntime.definition().chat().model()).isEqualTo("stored-chat-model");
        assertThat(providerRuntime.definition().embedding().baseUrl()).isEqualTo("https://stored-embedding.example.com/v1");
        assertThat(providerRuntime.definition().embedding().model()).isEqualTo("stored-embedding-model");
        assertThat(providerRuntime.definition().rerank().baseUrl()).isEqualTo("https://stored-rerank.example.com/v1");
        assertThat(providerRuntime.definition().rerank().model()).isEqualTo("stored-rerank-model");
    }

    @Test
    void shouldSupportCustomProviderKeysDuringBootstrap() {
        AiRuntimeConfigService service = runtimeConfigService(baseUrl, "test-internal-token", "primary_openai", "backup_1");

        assertThat(service.current().providerRuntime("primary_openai")).isNotNull();
        assertThat(service.current().providerRuntime("backup_1")).isNotNull();
        assertThat(service.current().providerRuntime("qwen")).isNull();
    }

    @Test
    void shouldRejectInvalidProviderKeyDuringValidation() {
        AiRuntimeConfigService service = runtimeConfigService(baseUrl, "test-internal-token");
        AiOpsConfigPayload payload = payload(baseUrl, "test-internal-token");
        payload = new AiOpsConfigPayload(
                new AiOpsProviderConfig(
                        payload.provider().activeProvider(),
                        payload.provider().fallbackProvider(),
                        Map.of(
                                "Primary OpenAI", payload.provider().providers().get("qwen"),
                                "deepseek", payload.provider().providers().get("deepseek")
                        )
                ),
                payload.resilience(),
                payload.rag()
        );

        var validation = service.validate(payload);

        assertThat(validation.valid()).isFalse();
        assertThat(validation.issues())
                .extracting(AiOpsConfigIssue::field)
                .contains("provider.providers.Primary OpenAI");
    }

    @Test
    void shouldRequireAtLeastTwoProviderDefinitionsWhenFallbackIsConfigured() {
        AiRuntimeConfigService service = runtimeConfigService(baseUrl, "test-internal-token");
        AiOpsConfigPayload payload = payload(baseUrl, "test-internal-token");
        payload = new AiOpsConfigPayload(
                new AiOpsProviderConfig(
                        payload.provider().activeProvider(),
                        payload.provider().fallbackProvider(),
                        Map.of("qwen", payload.provider().providers().get("qwen"))
                ),
                payload.resilience(),
                payload.rag()
        );

        var validation = service.validate(payload);

        assertThat(validation.valid()).isFalse();
        assertThat(validation.issues())
                .extracting(AiOpsConfigIssue::code)
                .contains("provider_count_requires_fallback");
    }

    @Test
    void shouldReturnValidationIssuesWhenFallbackProviderSectionIsMissing() {
        AiRuntimeConfigService service = runtimeConfigService(baseUrl, "test-internal-token");
        AiOpsConfigPayload basePayload = payload(baseUrl, "test-internal-token");
        AiOpsConfigPayload malformedPayload = new AiOpsConfigPayload(
                new AiOpsProviderConfig(
                        basePayload.provider().activeProvider(),
                        basePayload.provider().fallbackProvider(),
                        Map.of(
                                "qwen",
                                new AiOpsProviderDefinition(
                                        null,
                                        basePayload.provider().providers().get("qwen").embedding(),
                                        basePayload.provider().providers().get("qwen").rerank()
                                ),
                                "deepseek",
                                basePayload.provider().providers().get("deepseek")
                        )
                ),
                basePayload.resilience(),
                basePayload.rag()
        );

        var validation = service.validate(malformedPayload);

        assertThat(validation.valid()).isFalse();
        assertThat(validation.issues())
                .extracting(AiOpsConfigIssue::code)
                .contains("chat_section_required");
        assertThat(validation.notices())
                .extracting(notice -> notice.code())
                .contains("automatic_failover_enabled");
    }

    @Test
    void shouldDisableFailoverAndEmitErrorWhenFallbackResolvesToTheSameUpstream() {
        AiRuntimeConfigService service = runtimeConfigService(baseUrl, "test-internal-token");
        assertThat(service.isDistinctFallback("chat", service.current())).isFalse();
        assertThat(service.isDistinctFallback("embedding", service.current())).isFalse();
        assertThat(service.isDistinctFallback("rerank", service.current())).isFalse();

        AiOpsConfigPayload sameUpstream = payload(baseUrl, "test-internal-token");
        var qwen = sameUpstream.provider().providers().get("qwen");
        sameUpstream = new AiOpsConfigPayload(
                new AiOpsProviderConfig(
                        "qwen",
                        "deepseek",
                        Map.of("qwen", qwen, "deepseek", qwen)
                ),
                sameUpstream.resilience(),
                sameUpstream.rag()
        );

        StandardEnvironment prodEnvironment = new StandardEnvironment();
        prodEnvironment.setActiveProfiles("prod");
        AiRuntimeConfigService prodService = runtimeConfigService(baseUrl, "test-internal-token", "qwen", "deepseek", prodEnvironment);
        var validation = prodService.validate(sameUpstream);
        assertThat(validation.notices())
                .extracting(notice -> notice.code())
                .contains("fallback_same_upstream_all")
                .doesNotContain("automatic_failover_enabled");
        assertThat(validation.notices())
                .filteredOn(notice -> "fallback_same_upstream_all".equals(notice.code()))
                .extracting(notice -> notice.severity())
                .containsExactly("error");
    }

    @Test
    void shouldMarkStoredSyncFailedWhenStartupSyncFails() throws Exception {
        server.removeContext("/internal/ops/ai-config");
        try {
            server.createContext("/internal/ops/ai-config", exchange -> {
                byte[] body = "{\"success\":false}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });

            AiRuntimeConfigService service = runtimeConfigService(baseUrl, "test-internal-token");
            service.syncStoredConfigAfterStartup();

            assertThat(service.storedSyncStatus()).isEqualTo(AiRuntimeConfigService.STORED_SYNC_STATUS_SYNC_FAILED);
        } finally {
            server.removeContext("/internal/ops/ai-config");
            server.createContext("/internal/ops/ai-config", exchange -> {
                lastInternalToken.set(exchange.getRequestHeaders().getFirst(InternalApiHeaders.INTERNAL_TOKEN));
                byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
        }
    }

    @Test
    void shouldRecoverStoredSyncStatusWhenRetrySucceedsAfterStartupFailure() throws Exception {
        AiOpsConfigPayload payload = payload(baseUrl, "test-internal-token");
        server.removeContext("/internal/ops/ai-config");
        try {
            server.createContext("/internal/ops/ai-config", exchange -> {
                byte[] body = "{\"success\":false}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(503, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });

            AiRuntimeConfigService service = runtimeConfigService(baseUrl, "test-internal-token");
            service.syncStoredConfigAfterStartup();

            assertThat(service.storedSyncStatus()).isEqualTo(AiRuntimeConfigService.STORED_SYNC_STATUS_SYNC_FAILED);

            server.removeContext("/internal/ops/ai-config");
            server.createContext("/internal/ops/ai-config", exchange -> {
                AiOpsConfigEffectiveResponse effective = new AiOpsConfigEffectiveResponse(
                        payload,
                        "APP_SERVER_SYNC",
                        5L,
                        OffsetDateTime.parse("2026-03-21T00:00:00Z"),
                        List.of()
                );
                byte[] body = objectMapper.writeValueAsBytes(ApiResponse.success(effective, "trace-ai-runtime-sync"));
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });

            service.retryStoredConfigSyncIfFailed();

            assertThat(service.storedSyncStatus()).isEqualTo(AiRuntimeConfigService.STORED_SYNC_STATUS_IN_SYNC);
            assertThat(service.current().version()).isEqualTo(5L);
            assertThat(service.current().source()).isEqualTo("APP_SERVER_SYNC");
        } finally {
            server.removeContext("/internal/ops/ai-config");
            server.createContext("/internal/ops/ai-config", exchange -> {
                lastInternalToken.set(exchange.getRequestHeaders().getFirst(InternalApiHeaders.INTERNAL_TOKEN));
                byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
        }
    }

    @Test
    void shouldStageAndCommitRuntimeBundle() {
        AiRuntimeConfigService service = runtimeConfigService(baseUrl, "test-internal-token");
        AiOpsConfigPayload payload = payload(baseUrl, "test-internal-token");

        var staged = service.stage(payload, "DATABASE", 7L);
        var committed = service.commit(staged.stageId());

        assertThat(staged.version()).isEqualTo(7L);
        assertThat(committed.version()).isEqualTo(7L);
        assertThat(service.current().version()).isEqualTo(7L);
        assertThat(service.current().source()).isEqualTo("DATABASE");
    }

    @Test
    void shouldReturnCommittedResponseWhenStageIsCommittedTwice() {
        AiRuntimeConfigService service = runtimeConfigService(baseUrl, "test-internal-token");
        AiOpsConfigPayload payload = payload(baseUrl, "test-internal-token");

        var staged = service.stage(payload, "DATABASE", 11L);
        var firstCommit = service.commit(staged.stageId());
        var secondCommit = service.commit(staged.stageId());

        assertThat(secondCommit.version()).isEqualTo(firstCommit.version());
        assertThat(secondCommit.source()).isEqualTo(firstCommit.source());
    }

    @Test
    void shouldRejectCommitForStaleStagedBundleVersion() {
        AiRuntimeConfigService service = runtimeConfigService(baseUrl, "test-internal-token");
        AiOpsConfigPayload payload = payload(baseUrl, "test-internal-token");

        var staleStage = service.stage(payload, "DATABASE", 7L);
        var latestStage = service.stage(payload, "DATABASE", 8L);
        service.commit(latestStage.stageId());

        assertThatThrownBy(() -> service.commit(staleStage.stageId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("older than the active runtime version");
        assertThat(service.current().version()).isEqualTo(8L);
    }

    private AiRuntimeConfigService runtimeConfigService(String appServerBaseUrl, String internalToken) {
        return runtimeConfigService(appServerBaseUrl, internalToken, "qwen", "deepseek");
    }

    private AiRuntimeConfigService runtimeConfigService(
            String appServerBaseUrl,
            String internalToken,
            String activeProvider,
            String fallbackProvider
    ) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.setActiveProfiles("test");
        return runtimeConfigService(appServerBaseUrl, internalToken, activeProvider, fallbackProvider, environment);
    }

    private AiRuntimeConfigService runtimeConfigService(
            String appServerBaseUrl,
            String internalToken,
            String activeProvider,
            String fallbackProvider,
            org.springframework.core.env.Environment environment
    ) {
        AiProviderProperties providerProperties = new AiProviderProperties();
        providerProperties.setActiveProvider(activeProvider);
        providerProperties.setFallbackProvider(fallbackProvider);
        AiProviderProperties.ProviderProperties primary = new AiProviderProperties.ProviderProperties();
        primary.getChat().setBaseUrl("https://example.com/v1");
        primary.getChat().setApiKey("chat-key");
        primary.getChat().setModel("qwen-max");
        primary.getEmbedding().setBaseUrl("https://example.com/v1");
        primary.getEmbedding().setApiKey("embed-key");
        primary.getEmbedding().setModel("text-embedding-v4");
        primary.getEmbedding().setDimension(1024);
        primary.getRerank().setBaseUrl("https://example.com");
        primary.getRerank().setApiKey("rerank-key");
        primary.getRerank().setModel("gte-rerank-v2");
        providerProperties.getProviders().put(activeProvider, primary);
        providerProperties.getProviders().put(fallbackProvider, primary);

        AiResilienceProperties resilienceProperties = new AiResilienceProperties();
        RagProperties ragProperties = new RagProperties();
        ragProperties.getAppServer().setBaseUrl(appServerBaseUrl);
        ragProperties.getAppServer().setInternalToken(internalToken);

        AiRuntimeBundleFactory bundleFactory = new AiRuntimeBundleFactory(
                RestClient.builder(),
                (request, body, execution) -> execution.execute(request, body),
                new ProviderErrorSupport(objectMapper, new SensitiveDataRedactor())
        );
        RagSchemaDimensionGuard ragSchemaDimensionGuard = mock(RagSchemaDimensionGuard.class);
        doNothing().when(ragSchemaDimensionGuard).verifyConfig(org.mockito.ArgumentMatchers.any());
        AiRuntimeConfigService service = new AiRuntimeConfigService(
                providerProperties,
                resilienceProperties,
                ragProperties,
                bundleFactory,
                ragSchemaDimensionGuard,
                new SensitiveDataRedactor(),
                VALIDATOR,
                environment
        );
        ReflectionTestUtils.invokeMethod(service, "initialize");
        return service;
    }

    private AiOpsConfigPayload payload(String appServerBaseUrl, String internalToken) {
        return payload(
                appServerBaseUrl,
                internalToken,
                "https://example.com/v1",
                "qwen-max",
                "https://example.com/v1",
                "text-embedding-v4",
                "https://example.com",
                "gte-rerank-v2"
        );
    }

    private AiOpsConfigPayload payload(
            String appServerBaseUrl,
            String internalToken,
            String qwenChatBaseUrl,
            String qwenChatModel,
            String qwenEmbeddingBaseUrl,
            String qwenEmbeddingModel,
            String qwenRerankBaseUrl,
            String qwenRerankModel
    ) {
        return new AiOpsConfigPayload(
                new AiOpsProviderConfig(
                        "qwen",
                        "deepseek",
                        Map.of(
                                "qwen",
                                new AiOpsProviderDefinition(
                                        new AiOpsChatConfig(AiOpsProtocols.OPENAI_COMPAT, qwenChatBaseUrl, "chat-key", qwenChatModel, "PT3S", "PT30S", 0.2d, 1024),
                                        new AiOpsEmbeddingConfig(AiOpsProtocols.OPENAI_COMPAT, qwenEmbeddingBaseUrl, "embed-key", qwenEmbeddingModel, "PT3S", "PT30S", 1024),
                                        new AiOpsRerankConfig(AiOpsProtocols.OPENAI_RERANK, qwenRerankBaseUrl, "rerank-key", qwenRerankModel, "PT3S", "PT30S")
                                ),
                                "deepseek",
                                new AiOpsProviderDefinition(
                                        new AiOpsChatConfig(AiOpsProtocols.OPENAI_COMPAT, "https://example.com/v1", "backup-chat-key", "deepseek-chat", "PT3S", "PT30S", 0.2d, 1024),
                                        new AiOpsEmbeddingConfig(AiOpsProtocols.OPENAI_COMPAT, "https://example.com/v1", "backup-embed-key", qwenEmbeddingModel, "PT3S", "PT30S", 1024),
                                        new AiOpsRerankConfig(AiOpsProtocols.OPENAI_RERANK, "https://example.com", "backup-rerank-key", "gte-rerank-v2", "PT3S", "PT30S")
                                )
                        )
                ),
                new AiOpsResilienceConfig(3, "PT0.5S", 50.0f, 20, "PT30S"),
                new AiOpsRagConfig(
                        new AiOpsRagAppServerConfig(appServerBaseUrl, internalToken, "PT3S", "PT5S"),
                        new AiOpsRagIngestionConfig(100, 32),
                        new AiOpsRagRetrievalConfig(20, 0.55d, 8, 0.2d, 6, 64)
                )
        );
    }
}
