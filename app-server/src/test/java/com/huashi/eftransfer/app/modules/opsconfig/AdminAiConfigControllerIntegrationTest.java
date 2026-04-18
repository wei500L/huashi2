package com.huashi.eftransfer.app.modules.opsconfig;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.audit.entity.AuditLogEntity;
import com.huashi.eftransfer.app.common.audit.mapper.AuditLogMapper;
import com.huashi.eftransfer.app.common.config.AiGatewayClientProperties;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayCallResult;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayClient;
import com.huashi.eftransfer.shared.ai.AiGatewayHealthResponse;
import com.huashi.eftransfer.shared.ai.AdminAiEmbeddingProbeVO;
import com.huashi.eftransfer.shared.ai.AdminAiRerankProbeVO;
import com.huashi.eftransfer.app.modules.opsconfig.entity.AiOpsConfigHistoryEntity;
import com.huashi.eftransfer.app.modules.opsconfig.mapper.AiOpsConfigHistoryMapper;
import com.huashi.eftransfer.app.modules.opsconfig.service.AiOpsConfigStorageService;
import com.huashi.eftransfer.app.support.AbstractWebIntegrationTest;
import com.huashi.eftransfer.shared.ai.RagReindexJobResponse;
import com.huashi.eftransfer.shared.ai.RagReindexRequest;
import com.huashi.eftransfer.shared.ai.RagReindexResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsChatConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigApplyResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigIssue;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigNotice;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigStageResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigValidationResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(AdminAiConfigControllerIntegrationTest.StubConfig.class)
class AdminAiConfigControllerIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private StubAiGatewayClient stubAiGatewayClient;

    @Autowired
    private AiOpsConfigStorageService storageService;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Autowired
    private AiOpsConfigHistoryMapper aiOpsConfigHistoryMapper;

    @BeforeEach
    void resetGatewayStub() {
        stubAiGatewayClient.currentEffective = new AiOpsConfigEffectiveResponse(
                null,
                "DEFAULTS",
                1L,
                OffsetDateTime.now(),
                List.of()
        );
        stubAiGatewayClient.validationResponse = new AiOpsConfigValidationResponse(true, List.of(), List.of());
        stubAiGatewayClient.lastAppliedConfig = null;
        stubAiGatewayClient.stagedConfigs.clear();
        stubAiGatewayClient.stageSequence = 0L;
        stubAiGatewayClient.validationUnavailable = false;
        stubAiGatewayClient.stageUnavailable = false;
        stubAiGatewayClient.commitUnavailable = false;
    }

    @Test
    void adminCanSaveConfigAndRetainExistingSecret() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");
        long initialAuditCount = saveAuditCount();
        long initialHistoryCount = historyCount();
        stubAiGatewayClient.currentEffective = new AiOpsConfigEffectiveResponse(
                samplePayload("chat-secret-001"),
                "DEFAULTS",
                1L,
                OffsetDateTime.now(),
                List.of(new AiOpsConfigNotice(
                        "automatic_failover_enabled",
                        "info",
                        "Automatic failover is enabled for retryable provider failures and circuit-open scenarios."
                ))
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
        assertThat(saveAuditCount()).isEqualTo(initialAuditCount + 1);
        assertThat(historyCount()).isEqualTo(initialHistoryCount + 1);
        AiOpsConfigHistoryEntity history = latestHistory();
        assertThat(history.getVersionNumber()).isEqualTo(2L);
        assertThat(history.getPreviousVersionNumber()).isEqualTo(1L);
        AuditLogEntity auditLog = latestSaveAudit();
        assertThat(auditLog.getRequestPayload()).contains("\"previousVersion\":1");
        assertThat(auditLog.getRequestPayload()).contains("\"nextVersion\":2");
        assertThat(auditLog.getRequestPayload()).contains("\"configDiffs\"");
        assertThat(auditLog.getRequestPayload()).contains("\"secretChanges\"");

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
        assertThat(saveAuditCount()).isEqualTo(initialAuditCount + 1);
        assertThat(historyCount()).isEqualTo(initialHistoryCount + 1);
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
    void saveCanonicalizesProviderOrderBeforeApplyAndPersist() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");
        stubAiGatewayClient.currentEffective = new AiOpsConfigEffectiveResponse(
                samplePayload("chat-secret-001"),
                "DEFAULTS",
                1L,
                OffsetDateTime.now(),
                List.of()
        );

        AiOpsConfigPayload basePayload = samplePayload(null);
        LinkedHashMap<String, AiOpsProviderDefinition> providers = new LinkedHashMap<>();
        providers.put("archive", basePayload.provider().providers().get("qwen"));
        providers.put("qwen", basePayload.provider().providers().get("qwen"));
        providers.put("deepseek", basePayload.provider().providers().get("deepseek"));
        AiOpsConfigPayload reorderedPayload = new AiOpsConfigPayload(
                new AiOpsProviderConfig("deepseek", "qwen", providers),
                basePayload.resilience(),
                basePayload.rag()
        );

        mockMvc.perform(put("/api/admin/ai-config")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "config", reorderedPayload,
                                "expectedVersion", 1,
                                "secrets", Map.of(
                                        "providers", Map.of(
                                                "deepseek", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", true),
                                                        "embeddingApiKey", Map.of("retainExisting", true),
                                                        "rerankApiKey", Map.of("retainExisting", true)
                                                ),
                                                "qwen", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", true),
                                                        "embeddingApiKey", Map.of("retainExisting", true),
                                                        "rerankApiKey", Map.of("retainExisting", true)
                                                ),
                                                "archive", Map.of(
                                                        "chatApiKey", Map.of("retainExisting", false, "value", "archive-chat-secret"),
                                                        "embeddingApiKey", Map.of("retainExisting", false, "value", "archive-embed-secret"),
                                                        "rerankApiKey", Map.of("retainExisting", false, "value", "archive-rerank-secret")
                                                )
                                        ),
                                        "appServerInternalToken", Map.of("retainExisting", true)
                                )
                        ))))
                .andExpect(status().isOk());

        assertThat(List.copyOf(stubAiGatewayClient.lastAppliedConfig.provider().providers().keySet()))
                .containsExactly("deepseek", "qwen", "archive");
        assertThat(List.copyOf(storageService.load().orElseThrow().config().provider().providers().keySet()))
                .containsExactly("deepseek", "qwen", "archive");
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
                null,
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
                null,
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
    void blankDraftLoadsWhenNoStoredSnapshotAndRuntimeIsUnavailable() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");
        stubAiGatewayClient.currentEffective = null;

        mockMvc.perform(get("/api/admin/ai-config").with(bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.runtime.available").value(false))
                .andExpect(jsonPath("$.data.stored.present").value(false))
                .andExpect(jsonPath("$.data.config.provider.providers").value(org.hamcrest.Matchers.anEmptyMap()))
                .andExpect(jsonPath("$.data.config.rag.appServer").exists())
                .andExpect(jsonPath("$.data.config.resilience").exists())
                .andExpect(jsonPath("$.data.notices[0]").value(org.hamcrest.Matchers.containsString("No stored AI ops config exists yet")));
    }

    @Test
    void offlineSavePersistsDatabaseSnapshotAndLeavesRuntimePending() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");
        stubAiGatewayClient.currentEffective = null;
        stubAiGatewayClient.validationUnavailable = true;
        stubAiGatewayClient.stageUnavailable = true;

        mockMvc.perform(put("/api/admin/ai-config")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "config", samplePayload(null),
                                "expectedVersion", null,
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
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.runtime.available").value(false))
                .andExpect(jsonPath("$.data.runtime.inSync").value(false))
                .andExpect(jsonPath("$.data.stored.present").value(true))
                .andExpect(jsonPath("$.data.notices[*]").value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("runtime sync is pending"))));

        assertThat(storageService.load()).isPresent();
        assertThat(stubAiGatewayClient.lastAppliedConfig).isNull();
    }

    @Test
    void runtimeSyncEndpointAppliesStoredSnapshotToGateway() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");
        storageService.save(samplePayload("stored-chat-secret"), null, 5L, null);
        stubAiGatewayClient.currentEffective = new AiOpsConfigEffectiveResponse(
                samplePayload("runtime-chat-secret"),
                "DEFAULTS",
                4L,
                OffsetDateTime.now(),
                List.of()
        );

        mockMvc.perform(post("/api/admin/ai-config/runtime/sync")
                        .with(bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(5))
                .andExpect(jsonPath("$.data.runtime.available").value(true))
                .andExpect(jsonPath("$.data.runtime.version").value(5))
                .andExpect(jsonPath("$.data.runtime.inSync").value(true));

        assertThat(stubAiGatewayClient.lastAppliedConfig).isNotNull();
        assertThat(stubAiGatewayClient.lastAppliedConfig.provider().providers().get("qwen").chat().apiKey())
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

    @Test
    void embeddingProbeUsesDraftConfigAndReturnsGatewayResult() throws Exception {
        String adminToken = loginAndGetAccessToken("admin", "Admin@123456");
        stubAiGatewayClient.embeddingProbeResponse = new AdminAiEmbeddingProbeVO(
                true,
                "Embedding probe succeeded",
                "qwen",
                "text-embedding-v4",
                48L,
                "probe-embedding-1",
                OffsetDateTime.now(),
                1024,
                1024,
                1
        );

        mockMvc.perform(post("/api/admin/ai-config/probes/embedding")
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
                .andExpect(jsonPath("$.data.ok").value(true))
                .andExpect(jsonPath("$.data.dimension").value(1024));
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
                                                AiOpsProtocols.OPENAI_COMPAT,
                                                qwenChatBaseUrl,
                                                chatApiKey,
                                                qwenChatModel,
                                                "PT30S",
                                                0.2d,
                                                2048
                                        ),
                                        new AiOpsEmbeddingConfig(
                                                AiOpsProtocols.OPENAI_COMPAT,
                                                qwenEmbeddingBaseUrl,
                                                "embed-secret-001",
                                                qwenEmbeddingModel,
                                                "PT30S",
                                                1024
                                        ),
                                        new AiOpsRerankConfig(
                                                AiOpsProtocols.QWEN_RERANK,
                                                qwenRerankBaseUrl,
                                                "rerank-secret-001",
                                                qwenRerankModel,
                                                "PT30S"
                                        )
                                ),
                                "deepseek",
                                new AiOpsProviderDefinition(
                                        new AiOpsChatConfig(
                                                AiOpsProtocols.OPENAI_COMPAT,
                                                "https://api.deepseek.com/v1",
                                                "chat-secret-002",
                                                "deepseek-chat",
                                                "PT30S",
                                                0.2d,
                                                2048
                                        ),
                                        new AiOpsEmbeddingConfig(
                                                AiOpsProtocols.OPENAI_COMPAT,
                                                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                                                "embed-secret-002",
                                                "text-embedding-v4",
                                                "PT30S",
                                                1024
                                        ),
                                        new AiOpsRerankConfig(
                                                AiOpsProtocols.QWEN_RERANK,
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

        private record StagedConfig(AiOpsConfigPayload payload, String source, Long version) {
        }

        private AiOpsConfigEffectiveResponse currentEffective = new AiOpsConfigEffectiveResponse(
                null,
                "DEFAULTS",
                1L,
                OffsetDateTime.now(),
                List.of()
        );
        private AiOpsConfigValidationResponse validationResponse = new AiOpsConfigValidationResponse(true, List.of(), List.of());
        private AiOpsConfigPayload lastAppliedConfig;
        private final Map<String, StagedConfig> stagedConfigs = new LinkedHashMap<>();
        private long stageSequence = 0L;
        private boolean validationUnavailable;
        private boolean stageUnavailable;
        private boolean commitUnavailable;
        private AdminAiEmbeddingProbeVO embeddingProbeResponse = new AdminAiEmbeddingProbeVO(
                true, "Embedding probe succeeded", "qwen", "text-embedding-v4", 32L, "probe-embedding", OffsetDateTime.now(), 1024, 1024, 1
        );
        private AdminAiRerankProbeVO rerankProbeResponse = new AdminAiRerankProbeVO(
                true, "Rerank probe succeeded", "qwen", "gte-rerank-v2", 35L, "probe-rerank", OffsetDateTime.now(), 3, 3, true, 0, 0.98d
        );

        StubAiGatewayClient(RestClient aiGatewayRestClient, AiGatewayClientProperties properties) {
            super(aiGatewayRestClient, properties);
        }

        @Override
        public Optional<AiOpsConfigEffectiveResponse> fetchEffectiveConfig() {
            return Optional.ofNullable(currentEffective);
        }

        @Override
        public AiOpsConfigValidationResponse validateConfig(AiOpsConfigPayload payload) {
            if (validationUnavailable) {
                throw new IllegalStateException("validation unavailable");
            }
            return validationResponse;
        }

        @Override
        public AiOpsConfigApplyResponse applyConfig(AiOpsConfigPayload payload, String source, Long version) {
            AiOpsConfigStageResponse staged = stageConfig(payload, source, version);
            return commitConfig(staged.stageId());
        }

        @Override
        public AiOpsConfigStageResponse stageConfig(AiOpsConfigPayload payload, String source, Long version) {
            if (stageUnavailable) {
                throw new IllegalStateException("stage unavailable");
            }
            long stagedVersion = version == null ? (currentEffective == null || currentEffective.version() == null ? 1L : currentEffective.version() + 1L) : version;
            String stagedSource = source == null ? "ADMIN_STAGE" : source;
            String stageId = "stage-" + (++stageSequence);
            stagedConfigs.put(stageId, new StagedConfig(payload, stagedSource, stagedVersion));
            return new AiOpsConfigStageResponse(stageId, stagedSource, stagedVersion, OffsetDateTime.now(), List.of());
        }

        @Override
        public AiOpsConfigApplyResponse commitConfig(String stageId) {
            if (commitUnavailable) {
                throw new IllegalStateException("commit unavailable");
            }
            StagedConfig staged = stagedConfigs.remove(stageId);
            if (staged == null) {
                throw new IllegalStateException("stage not found");
            }
            lastAppliedConfig = staged.payload();
            currentEffective = new AiOpsConfigEffectiveResponse(
                    staged.payload(),
                    staged.source(),
                    staged.version(),
                    OffsetDateTime.now(),
                    List.of()
            );
            return new AiOpsConfigApplyResponse(staged.source(), staged.version(), OffsetDateTime.now(), List.of());
        }

        @Override
        public Optional<AiGatewayHealthResponse> fetchHealth() {
            return Optional.of(new AiGatewayHealthResponse(
                    "ai-gateway",
                    "UP",
                    "IN_SYNC",
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
        public AiGatewayCallResult<AdminAiEmbeddingProbeVO> probeEmbeddingConfig(AiOpsConfigPayload payload) {
            return AiGatewayCallResult.success(embeddingProbeResponse, 1, embeddingProbeResponse.latencyMs(), "/internal/ai/config/probes/embedding");
        }

        @Override
        public AiGatewayCallResult<AdminAiRerankProbeVO> probeRerankConfig(AiOpsConfigPayload payload) {
            return AiGatewayCallResult.success(rerankProbeResponse, 1, rerankProbeResponse.latencyMs(), "/internal/ai/config/probes/rerank");
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

    private long saveAuditCount() {
        return auditLogMapper.selectCount(Wrappers.<AuditLogEntity>lambdaQuery()
                .eq(AuditLogEntity::getActionType, "ai_ops_config_save"));
    }

    private long historyCount() {
        return aiOpsConfigHistoryMapper.selectCount(Wrappers.<AiOpsConfigHistoryEntity>lambdaQuery()
                .eq(AiOpsConfigHistoryEntity::getConfigKey, AiOpsConfigStorageService.CONFIG_KEY));
    }

    private AuditLogEntity latestSaveAudit() {
        return auditLogMapper.selectOne(Wrappers.<AuditLogEntity>lambdaQuery()
                .eq(AuditLogEntity::getActionType, "ai_ops_config_save")
                .orderByDesc(AuditLogEntity::getId)
                .last("LIMIT 1"));
    }

    private AiOpsConfigHistoryEntity latestHistory() {
        return aiOpsConfigHistoryMapper.selectOne(Wrappers.<AiOpsConfigHistoryEntity>lambdaQuery()
                .eq(AiOpsConfigHistoryEntity::getConfigKey, AiOpsConfigStorageService.CONFIG_KEY)
                .orderByDesc(AiOpsConfigHistoryEntity::getId)
                .last("LIMIT 1"));
    }
}
