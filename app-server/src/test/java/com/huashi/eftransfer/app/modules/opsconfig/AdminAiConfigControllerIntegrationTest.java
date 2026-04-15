package com.huashi.eftransfer.app.modules.opsconfig;

import com.huashi.eftransfer.app.common.config.AiGatewayClientProperties;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayCallResult;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayClient;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayFailureReason;
import com.huashi.eftransfer.shared.ai.AiGatewayHealthResponse;
import com.huashi.eftransfer.app.modules.opsconfig.service.AiOpsConfigStorageService;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import com.huashi.eftransfer.shared.ai.RagReindexJobResponse;
import com.huashi.eftransfer.shared.ai.RagReindexRequest;
import com.huashi.eftransfer.shared.ai.RagReindexResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsChatConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigApplyResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigIssue;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigValidationResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsEmbeddingConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsProviderConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsProviderDefinition;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagAppServerConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagIngestionConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagRetrievalConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRerankConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsResilienceConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(AdminAiConfigControllerIntegrationTest.StubConfig.class)
class AdminAiConfigControllerIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private StubAiGatewayClient stubAiGatewayClient;

    @Autowired
    private AiOpsConfigStorageService storageService;

    @Test
    void adminCanSaveConfigAndRetainExistingSecret() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");
        stubAiGatewayClient.currentEffective = new AiOpsConfigEffectiveResponse(
                samplePayload("chat-secret-001"),
                "DEFAULTS",
                1L,
                OffsetDateTime.now(),
                List.of("Automatic failover is enabled for retryable provider failures and circuit-open scenarios.")
        );

        mockMvc.perform(put("/api/admin/ai-config")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "config", samplePayload(null),
                                "expectedVersion", 1,
                                "secrets", Map.of(
                                        "providers", Map.of(
                                                "qwen", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", false, "value", "chat-secret-001"),
                                                        "embeddingApiKey", Map.of("retainExisting", false, "value", "embed-secret-001"),
                                                        "rerankApiKey", Map.of("retainExisting", false, "value", "rerank-secret-001")
                                                ),
                                                "deepseek", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", false, "value", "chat-secret-002"),
                                                        "embeddingApiKey", Map.of("retainExisting", false, "value", "embed-secret-002"),
                                                        "rerankApiKey", Map.of("retainExisting", false, "value", "rerank-secret-002")
                                                )
                                        ),
                                        "appServerInternalToken", Map.of("retainExisting", false, "value", "internal-token-001")
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.config.provider.providers.qwen.chat.apiKey").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.secrets.providers.qwen.chatApiKey.configured").value(true));

        assertThat(storageService.load()).isPresent();
        assertThat(storageService.load().orElseThrow().config().provider().providers().get("qwen").chat().apiKey()).isEqualTo("chat-secret-001");

        mockMvc.perform(put("/api/admin/ai-config")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "config", samplePayload(null),
                                "expectedVersion", 2,
                                "secrets", Map.of(
                                        "providers", Map.of(
                                                "qwen", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", true),
                                                        "embeddingApiKey", Map.of("retainExisting", true),
                                                        "rerankApiKey", Map.of("retainExisting", true)
                                                ),
                                                "deepseek", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", true),
                                                        "embeddingApiKey", Map.of("retainExisting", true),
                                                        "rerankApiKey", Map.of("retainExisting", true)
                                                )
                                        ),
                                        "appServerInternalToken", Map.of("retainExisting", true)
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.secrets.providers.qwen.chatApiKey.maskedValue").value(org.hamcrest.Matchers.containsString("cha")));

        assertThat(stubAiGatewayClient.lastAppliedConfig.provider().providers().get("qwen").chat().apiKey()).isEqualTo("chat-secret-001");
        assertThat(storageService.load().orElseThrow().config().provider().providers().get("qwen").chat().apiKey()).isEqualTo("chat-secret-001");
    }

    @Test
    void renameProviderRetainsExistingSecretsViaProviderOrigins() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");
        stubAiGatewayClient.currentEffective = new AiOpsConfigEffectiveResponse(
                samplePayload("chat-secret-001"),
                "DEFAULTS",
                1L,
                OffsetDateTime.now(),
                List.of()
        );

        mockMvc.perform(put("/api/admin/ai-config")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "config", samplePayload(null),
                                "expectedVersion", 1,
                                "secrets", Map.of(
                                        "providers", Map.of(
                                                "qwen", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", false, "value", "chat-secret-001"),
                                                        "embeddingApiKey", Map.of("retainExisting", false, "value", "embed-secret-001"),
                                                        "rerankApiKey", Map.of("retainExisting", false, "value", "rerank-secret-001")
                                                ),
                                                "deepseek", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", false, "value", "chat-secret-002"),
                                                        "embeddingApiKey", Map.of("retainExisting", false, "value", "embed-secret-002"),
                                                        "rerankApiKey", Map.of("retainExisting", false, "value", "rerank-secret-002")
                                                )
                                        ),
                                        "appServerInternalToken", Map.of("retainExisting", false, "value", "internal-token-001")
                                )
                        ))))
                .andExpect(status().isOk());

        AiOpsConfigPayload renamedPayload = samplePayload(null);
        renamedPayload = new AiOpsConfigPayload(
                new AiOpsProviderConfig(
                        "primary_openai",
                        "deepseek",
                        Map.of(
                                "primary_openai", renamedPayload.provider().providers().get("qwen"),
                                "deepseek", renamedPayload.provider().providers().get("deepseek")
                        )
                ),
                renamedPayload.resilience(),
                renamedPayload.rag()
        );

        mockMvc.perform(put("/api/admin/ai-config")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "config", renamedPayload,
                                "expectedVersion", 2,
                                "providerOrigins", Map.of("primary_openai", "qwen"),
                                "secrets", Map.of(
                                        "providers", Map.of(
                                                "primary_openai", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", true),
                                                        "embeddingApiKey", Map.of("retainExisting", true),
                                                        "rerankApiKey", Map.of("retainExisting", true)
                                                ),
                                                "deepseek", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", true),
                                                        "embeddingApiKey", Map.of("retainExisting", true),
                                                        "rerankApiKey", Map.of("retainExisting", true)
                                                )
                                        ),
                                        "appServerInternalToken", Map.of("retainExisting", true)
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.config.provider.activeProvider").value("primary_openai"))
                .andExpect(jsonPath("$.data.secrets.providers.primary_openai.chatApiKey.maskedValue").value(org.hamcrest.Matchers.containsString("cha")));

        assertThat(stubAiGatewayClient.lastAppliedConfig.provider().providers()).containsKey("primary_openai");
        assertThat(stubAiGatewayClient.lastAppliedConfig.provider().providers()).doesNotContainKey("qwen");
        assertThat(stubAiGatewayClient.lastAppliedConfig.provider().providers().get("primary_openai").chat().apiKey()).isEqualTo("chat-secret-001");
        assertThat(storageService.load().orElseThrow().config().provider().providers().get("primary_openai").chat().apiKey()).isEqualTo("chat-secret-001");
    }

    @Test
    void saveRejectsUnknownProviderOrigins() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");
        stubAiGatewayClient.currentEffective = new AiOpsConfigEffectiveResponse(
                samplePayload("chat-secret-001"),
                "DEFAULTS",
                1L,
                OffsetDateTime.now(),
                List.of()
        );

        AiOpsConfigPayload renamedPayload = samplePayload(null);
        renamedPayload = new AiOpsConfigPayload(
                new AiOpsProviderConfig(
                        "primary_openai",
                        "deepseek",
                        Map.of(
                                "primary_openai", renamedPayload.provider().providers().get("qwen"),
                                "deepseek", renamedPayload.provider().providers().get("deepseek")
                        )
                ),
                renamedPayload.resilience(),
                renamedPayload.rag()
        );

        mockMvc.perform(put("/api/admin/ai-config")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "config", renamedPayload,
                                "expectedVersion", 1,
                                "providerOrigins", Map.of("primary_openai", "missing_provider"),
                                "secrets", Map.of(
                                        "providers", Map.of(
                                                "primary_openai", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", true),
                                                        "embeddingApiKey", Map.of("retainExisting", true),
                                                        "rerankApiKey", Map.of("retainExisting", true)
                                                ),
                                                "deepseek", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", true),
                                                        "embeddingApiKey", Map.of("retainExisting", true),
                                                        "rerankApiKey", Map.of("retainExisting", true)
                                                )
                                        ),
                                        "appServerInternalToken", Map.of("retainExisting", true)
                                )
                        ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void configCenterPrefersStoredSnapshotOverRuntimeDefaults() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");
        storageService.save(
                samplePayload(
                        "stored-chat-secret",
                        "https://stored-chat.example.com/v1",
                        "stored-chat-model",
                        "https://stored-embedding.example.com/v1",
                        "stored-embedding-model",
                        "https://stored-rerank.example.com/v1/rerank",
                        "stored-rerank-model"
                ),
                5L,
                null
        );
        stubAiGatewayClient.currentEffective = new AiOpsConfigEffectiveResponse(
                samplePayload(
                        "runtime-chat-secret",
                        "https://runtime-chat.example.com/v1",
                        "runtime-chat-model",
                        "https://runtime-embedding.example.com/v1",
                        "runtime-embedding-model",
                        "https://runtime-rerank.example.com/v1/rerank",
                        "runtime-rerank-model"
                ),
                "DEFAULTS",
                4L,
                OffsetDateTime.now(),
                List.of()
        );

        mockMvc.perform(get("/api/admin/ai-config").with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("DATABASE"))
                .andExpect(jsonPath("$.data.version").value(5))
                .andExpect(jsonPath("$.data.config").exists())
                .andExpect(jsonPath("$.data.secrets").exists())
                .andExpect(jsonPath("$.data.runtime.available").isBoolean())
                .andExpect(jsonPath("$.data.stored.present").isBoolean())
                .andExpect(jsonPath("$.data.runtime.inSync").value(false))
                .andExpect(jsonPath("$.data.config.provider.providers.qwen.chat.baseUrl").value("https://stored-chat.example.com/v1"))
                .andExpect(jsonPath("$.data.config.provider.providers.qwen.chat.model").value("stored-chat-model"))
                .andExpect(jsonPath("$.data.config.provider.providers.qwen.embedding.baseUrl").value("https://stored-embedding.example.com/v1"))
                .andExpect(jsonPath("$.data.config.provider.providers.qwen.embedding.model").value("stored-embedding-model"))
                .andExpect(jsonPath("$.data.config.provider.providers.qwen.rerank.baseUrl").value("https://stored-rerank.example.com/v1/rerank"))
                .andExpect(jsonPath("$.data.config.provider.providers.qwen.rerank.model").value("stored-rerank-model"));
    }

    @Test
    void saveRetainExistingSecretsUsesStoredSnapshotWhenRuntimeIsOutOfSync() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");
        storageService.save(
                samplePayload(
                        "stored-chat-secret",
                        "https://stored-chat.example.com/v1",
                        "stored-chat-model",
                        "https://stored-embedding.example.com/v1",
                        "stored-embedding-model",
                        "https://stored-rerank.example.com/v1/rerank",
                        "stored-rerank-model"
                ),
                5L,
                null
        );
        stubAiGatewayClient.currentEffective = new AiOpsConfigEffectiveResponse(
                samplePayload(
                        "runtime-chat-secret",
                        "https://runtime-chat.example.com/v1",
                        "runtime-chat-model",
                        "https://runtime-embedding.example.com/v1",
                        "runtime-embedding-model",
                        "https://runtime-rerank.example.com/v1/rerank",
                        "runtime-rerank-model"
                ),
                "DEFAULTS",
                4L,
                OffsetDateTime.now(),
                List.of()
        );

        mockMvc.perform(put("/api/admin/ai-config")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "config", samplePayload(
                                        null,
                                        "https://stored-chat.example.com/v1",
                                        "stored-chat-model",
                                        "https://stored-embedding.example.com/v1",
                                        "stored-embedding-model",
                                        "https://stored-rerank.example.com/v1/rerank",
                                        "stored-rerank-model"
                                ),
                                "expectedVersion", 5,
                                "secrets", Map.of(
                                        "providers", Map.of(
                                                "qwen", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", true),
                                                        "embeddingApiKey", Map.of("retainExisting", true),
                                                        "rerankApiKey", Map.of("retainExisting", true)
                                                ),
                                                "deepseek", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", true),
                                                        "embeddingApiKey", Map.of("retainExisting", true),
                                                        "rerankApiKey", Map.of("retainExisting", true)
                                                )
                                        ),
                                        "appServerInternalToken", Map.of("retainExisting", true)
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(6))
                .andExpect(jsonPath("$.data.config.provider.providers.qwen.chat.baseUrl").value("https://stored-chat.example.com/v1"))
                .andExpect(jsonPath("$.data.config.provider.providers.qwen.chat.model").value("stored-chat-model"));

        assertThat(stubAiGatewayClient.lastAppliedConfig.provider().providers().get("qwen").chat().apiKey())
                .isEqualTo("stored-chat-secret");
        assertThat(stubAiGatewayClient.lastAppliedConfig.provider().providers().get("qwen").chat().baseUrl())
                .isEqualTo("https://stored-chat.example.com/v1");
        assertThat(stubAiGatewayClient.lastAppliedConfig.provider().providers().get("qwen").chat().model())
                .isEqualTo("stored-chat-model");
        assertThat(storageService.load().orElseThrow().config().provider().providers().get("qwen").chat().apiKey())
                .isEqualTo("stored-chat-secret");
    }

    @Test
    void nonAdminCannotAccessConfigCenter() throws Exception {
        String teacherToken = loginAndGetAccessToken("teacher.zhang", "Teacher@123456");

        mockMvc.perform(get("/api/admin/ai-config").with(bearer(teacherToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void validationFailureDoesNotPersistConfig() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");
        stubAiGatewayClient.currentEffective = new AiOpsConfigEffectiveResponse(
                samplePayload("chat-secret-001"),
                "DEFAULTS",
                1L,
                OffsetDateTime.now(),
                List.of()
        );
        stubAiGatewayClient.validationResponse = new AiOpsConfigValidationResponse(
                false,
                List.of(new AiOpsConfigIssue("provider.providers.Primary OpenAI", "provider key must contain only lowercase letters, numbers, hyphen, or underscore")),
                List.of()
        );

        mockMvc.perform(put("/api/admin/ai-config")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "config", samplePayload(null),
                                "expectedVersion", 1,
                                "secrets", Map.of(
                                        "providers", Map.of(
                                                "qwen", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", false, "value", "chat-secret-001"),
                                                        "embeddingApiKey", Map.of("retainExisting", false, "value", "embed-secret-001"),
                                                        "rerankApiKey", Map.of("retainExisting", false, "value", "rerank-secret-001")
                                                ),
                                                "deepseek", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", false, "value", "chat-secret-002"),
                                                        "embeddingApiKey", Map.of("retainExisting", false, "value", "embed-secret-002"),
                                                        "rerankApiKey", Map.of("retainExisting", false, "value", "rerank-secret-002")
                                                )
                                        ),
                                        "appServerInternalToken", Map.of("retainExisting", false, "value", "internal-token-001")
                                )
                        ))))
                .andExpect(status().isBadRequest());

        assertThat(storageService.load()).isEmpty();
    }

    private AiOpsConfigPayload samplePayload(String chatApiKey) {
        return samplePayload(
                chatApiKey,
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "qwen-max",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "text-embedding-v4",
                "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank",
                "gte-rerank-v2"
        );
    }

    private AiOpsConfigPayload samplePayload(
            String chatApiKey,
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
                                        new AiOpsChatConfig(
                                                qwenChatBaseUrl,
                                                chatApiKey,
                                                qwenChatModel,
                                                "PT30S",
                                                0.2d,
                                                2048
                                        ),
                                        new AiOpsEmbeddingConfig(
                                                qwenEmbeddingBaseUrl,
                                                "embed-secret-001",
                                                qwenEmbeddingModel,
                                                "PT30S",
                                                1024
                                        ),
                                        new AiOpsRerankConfig(
                                                qwenRerankBaseUrl,
                                                "rerank-secret-001",
                                                qwenRerankModel,
                                                "PT30S"
                                        )
                                ),
                                "deepseek",
                                new AiOpsProviderDefinition(
                                        new AiOpsChatConfig(
                                                "https://api.deepseek.com/v1",
                                                "chat-secret-002",
                                                "deepseek-chat",
                                                "PT30S",
                                                0.2d,
                                                2048
                                        ),
                                        new AiOpsEmbeddingConfig(
                                                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                                                "embed-secret-002",
                                                "text-embedding-v4",
                                                "PT30S",
                                                1024
                                        ),
                                        new AiOpsRerankConfig(
                                                "https://dashscope.aliyuncs.com/api/v1/services/rerank/text-rerank/text-rerank",
                                                "rerank-secret-002",
                                                "gte-rerank-v2",
                                                "PT30S"
                                        )
                                )
                        )
                ),
                new AiOpsResilienceConfig(3, "PT0.5S", 50.0f, 20, "PT30S"),
                new AiOpsRagConfig(
                        new AiOpsRagAppServerConfig(
                                "http://localhost:8080",
                                "internal-token-001",
                                "PT3S",
                                "PT5S"
                        ),
                        new AiOpsRagIngestionConfig(100, 32),
                        new AiOpsRagRetrievalConfig(20, 0.55d, 8, 0.2d, 6)
                )
        );
    }

    @TestConfiguration
    static class StubConfig {

        @Bean
        @Primary
        StubAiGatewayClient stubAiGatewayClient(RestClient aiGatewayRestClient, AiGatewayClientProperties properties) {
            return new StubAiGatewayClient(aiGatewayRestClient, properties);
        }
    }

    static class StubAiGatewayClient extends AiGatewayClient {

        private AiOpsConfigEffectiveResponse currentEffective = new AiOpsConfigEffectiveResponse(
                null,
                "DEFAULTS",
                1L,
                OffsetDateTime.now(),
                List.of()
        );
        private AiOpsConfigValidationResponse validationResponse = new AiOpsConfigValidationResponse(true, List.of(), List.of());
        private AiOpsConfigPayload lastAppliedConfig;

        StubAiGatewayClient(RestClient aiGatewayRestClient, AiGatewayClientProperties properties) {
            super(aiGatewayRestClient, properties);
        }

        @Override
        public Optional<AiOpsConfigEffectiveResponse> fetchEffectiveConfig() {
            return Optional.ofNullable(currentEffective);
        }

        @Override
        public AiOpsConfigValidationResponse validateConfig(AiOpsConfigPayload payload) {
            return validationResponse;
        }

        @Override
        public AiOpsConfigApplyResponse applyConfig(AiOpsConfigPayload payload, String source, Long version) {
            lastAppliedConfig = payload;
            long appliedVersion = version == null ? 2L : version;
            String appliedSource = source == null ? "ADMIN_APPLY" : source;
            currentEffective = new AiOpsConfigEffectiveResponse(payload, appliedSource, appliedVersion, OffsetDateTime.now(), List.of());
            return new AiOpsConfigApplyResponse(appliedSource, appliedVersion, OffsetDateTime.now(), List.of());
        }

        @Override
        public Optional<AiGatewayHealthResponse> fetchHealth() {
            return Optional.of(new AiGatewayHealthResponse(
                    "ai-gateway",
                    "UP",
                    "qwen",
                    "deepseek",
                    "qwen-max",
                    "text-embedding-v4",
                    "gte-rerank-v2",
                    true,
                    true,
                    true,
                    true,
                    true,
                    "0.5.1",
                    List.of("test"),
                    OffsetDateTime.now(),
                    null
            ));
        }

        @Override
        public AiGatewayCallResult<RagReindexResponse> reindex(RagReindexRequest request) {
            return AiGatewayCallResult.success(new RagReindexResponse(9L, "PENDING"), 1, 5L, "/internal/ai/rag/reindex");
        }

        @Override
        public Optional<RagReindexJobResponse> fetchReindexJob(Long jobId) {
            return Optional.of(new RagReindexJobResponse(
                    jobId,
                    "KNOWLEDGE_REINDEX",
                    "INCREMENTAL",
                    "PENDING",
                    List.of("LEXICAL_PAIR"),
                    List.of(),
                    null,
                    null,
                    null,
                    Map.of("documentsProcessed", 0),
                    null
            ));
        }
    }
}
