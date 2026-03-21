package com.huashi.eftransfer.app.modules.opsconfig.service;

import com.huashi.eftransfer.app.common.outbox.PlatformEventOutboxRecord;
import com.huashi.eftransfer.app.common.outbox.PlatformEventOutboxService;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayCallResult;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayClient;
import com.huashi.eftransfer.app.integration.ai.dto.AiGatewayHealthResponse;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiConfigSaveRequest;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiConfigViewVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiProviderSecretFieldsVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiProviderSecretUpdateGroup;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretFieldVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretFieldsVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretUpdateGroup;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretValueUpdate;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminOutboxRecordVO;
import com.huashi.eftransfer.app.modules.opsconfig.support.StoredAiOpsConfig;
import com.huashi.eftransfer.shared.ai.RagReindexJobResponse;
import com.huashi.eftransfer.shared.ai.RagReindexRequest;
import com.huashi.eftransfer.shared.ai.RagReindexResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsChatConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigValidationResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsEmbeddingConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsProviderConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsProviderDefinition;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagAppServerConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRerankConfig;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AiOpsAdminService {

    private final AiOpsConfigStorageService storageService;
    private final AiGatewayClient aiGatewayClient;
    private final PlatformEventOutboxService outboxService;

    public AiOpsAdminService(
            AiOpsConfigStorageService storageService,
            AiGatewayClient aiGatewayClient,
            PlatformEventOutboxService outboxService
    ) {
        this.storageService = storageService;
        this.aiGatewayClient = aiGatewayClient;
        this.outboxService = outboxService;
    }

    public AdminAiConfigViewVO getCurrentConfig() {
        Optional<StoredAiOpsConfig> stored = storageService.load();
        if (stored.isPresent()) {
            return toView(stored.get().config(), "DATABASE", stored.get().version(), stored.get().updatedAt(), List.of());
        }

        AiOpsConfigEffectiveResponse effective = aiGatewayClient.fetchEffectiveConfig()
                .orElseThrow(() -> new BusinessException(
                        ResultCode.AI_PROVIDER_UNAVAILABLE,
                        "ai-gateway runtime config is unavailable",
                        503
                ));
        return toView(
                effective.config(),
                effective.source(),
                effective.version(),
                effective.appliedAt(),
                effective.notices() == null ? List.of() : effective.notices()
        );
    }

    public AiOpsConfigValidationResponse validate(AdminAiConfigSaveRequest request) {
        AiOpsConfigPayload baseConfig = resolveBaseConfig();
        AiOpsConfigPayload candidate = mergeSecrets(baseConfig, request.config(), request.secrets());
        try {
            return aiGatewayClient.validateConfig(candidate);
        } catch (RuntimeException ex) {
            throw new BusinessException(ResultCode.AI_PROVIDER_UNAVAILABLE, "ai-gateway validation is unavailable", 503);
        }
    }

    @Transactional
    public AdminAiConfigViewVO save(AdminAiConfigSaveRequest request) {
        AiOpsConfigEffectiveResponse currentEffective = aiGatewayClient.fetchEffectiveConfig()
                .orElseThrow(() -> new BusinessException(
                        ResultCode.AI_PROVIDER_UNAVAILABLE,
                        "ai-gateway runtime config is unavailable",
                        503
                ));

        AiOpsConfigPayload baseConfig = storageService.load()
                .map(StoredAiOpsConfig::config)
                .orElse(currentEffective.config());
        AiOpsConfigPayload candidate = mergeSecrets(baseConfig, request.config(), request.secrets());
        AiOpsConfigValidationResponse validation;
        try {
            validation = aiGatewayClient.validateConfig(candidate);
        } catch (RuntimeException ex) {
            throw new BusinessException(ResultCode.AI_PROVIDER_UNAVAILABLE, "ai-gateway validation is unavailable", 503);
        }
        if (!validation.valid()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "AI ops config validation failed", 400);
        }

        try {
            aiGatewayClient.applyConfig(candidate);
        } catch (RuntimeException ex) {
            throw new BusinessException(ResultCode.AI_PROVIDER_UNAVAILABLE, "ai-gateway apply is unavailable", 503);
        }
        try {
            StoredAiOpsConfig stored = storageService.save(candidate, SecurityUtils.getCurrentUserId().orElse(null));
            return toView(candidate, "DATABASE", stored.version(), stored.updatedAt(), validation.notices());
        } catch (RuntimeException ex) {
            rollbackRuntime(currentEffective.config());
            throw ex;
        }
    }

    public AiGatewayHealthResponse health() {
        return aiGatewayClient.fetchHealth()
                .orElseThrow(() -> new BusinessException(
                        ResultCode.AI_PROVIDER_UNAVAILABLE,
                        "ai-gateway health is unavailable",
                        503
                ));
    }

    public RagReindexResponse triggerReindex(RagReindexRequest request) {
        AiGatewayCallResult<RagReindexResponse> result = aiGatewayClient.reindex(request);
        if (!result.success()) {
            throw new BusinessException(ResultCode.AI_PROVIDER_UNAVAILABLE, result.failureMessage(), 503);
        }
        return result.data();
    }

    public RagReindexJobResponse fetchReindexJob(Long jobId) {
        return aiGatewayClient.fetchReindexJob(jobId)
                .orElseThrow(() -> new BusinessException(
                        ResultCode.AI_PROVIDER_UNAVAILABLE,
                        "RAG reindex job is unavailable",
                        503
                ));
    }

    public List<AdminOutboxRecordVO> listOutbox(String status, Integer limit) {
        int boundedLimit = limit == null ? 20 : Math.min(Math.max(limit, 1), 100);
        return outboxService.list(status, boundedLimit).stream()
                .map(this::toOutboxView)
                .toList();
    }

    public AdminOutboxRecordVO replayOutbox(Long id) {
        try {
            return toOutboxView(outboxService.replay(id));
        } catch (IllegalStateException ex) {
            throw new BusinessException(ResultCode.NOT_FOUND, ex.getMessage(), 404);
        }
    }

    public AiOpsConfigEffectiveResponse getStoredConfigForInternalSync() {
        return storageService.load()
                .map(stored -> new AiOpsConfigEffectiveResponse(
                        stored.config(),
                        "DATABASE",
                        stored.version(),
                        stored.updatedAt(),
                        List.of()
                ))
                .orElse(null);
    }

    private void rollbackRuntime(AiOpsConfigPayload previousConfig) {
        try {
            aiGatewayClient.applyConfig(previousConfig);
        } catch (RuntimeException rollbackEx) {
            throw new TransactionSystemException("Failed to rollback ai-gateway runtime config after persistence failure", rollbackEx);
        }
    }

    private AiOpsConfigPayload resolveBaseConfig() {
        return storageService.load()
                .map(StoredAiOpsConfig::config)
                .orElseGet(() -> aiGatewayClient.fetchEffectiveConfig()
                        .map(AiOpsConfigEffectiveResponse::config)
                        .orElseThrow(() -> new BusinessException(
                                ResultCode.AI_PROVIDER_UNAVAILABLE,
                                "ai-gateway runtime config is unavailable",
                                503
                        )));
    }

    private AdminAiConfigViewVO toView(
            AiOpsConfigPayload payload,
            String source,
            Long version,
            java.time.OffsetDateTime updatedAt,
            List<String> notices
    ) {
        return new AdminAiConfigViewVO(
                sanitize(payload),
                buildSecrets(payload),
                source,
                version,
                updatedAt,
                notices == null ? List.of() : notices
        );
    }

    private AiOpsConfigPayload sanitize(AiOpsConfigPayload payload) {
        if (payload == null) {
            return null;
        }
        return new AiOpsConfigPayload(
                new AiOpsProviderConfig(
                        payload.provider().activeProvider(),
                        payload.provider().fallbackProvider(),
                        sanitizeProviders(payload.provider().providers())
                ),
                payload.resilience(),
                new AiOpsRagConfig(
                        new AiOpsRagAppServerConfig(
                                payload.rag().appServer().baseUrl(),
                                null,
                                payload.rag().appServer().connectTimeout(),
                                payload.rag().appServer().readTimeout()
                        ),
                        payload.rag().ingestion(),
                        payload.rag().retrieval()
                )
        );
    }

    private AdminAiSecretFieldsVO buildSecrets(AiOpsConfigPayload payload) {
        return new AdminAiSecretFieldsVO(
                buildProviderSecrets(payload.provider().providers()),
                mask(payload.rag().appServer().internalToken())
        );
    }

    private AdminAiSecretFieldVO mask(String value) {
        return new AdminAiSecretFieldVO(StringUtils.hasText(value), maskValue(value));
    }

    private String maskValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 6) {
            return "******";
        }
        return trimmed.substring(0, 3) + "******" + trimmed.substring(trimmed.length() - 3);
    }

    private AiOpsConfigPayload mergeSecrets(
            AiOpsConfigPayload base,
            AiOpsConfigPayload requestPayload,
            AdminAiSecretUpdateGroup secrets
    ) {
        AdminAiSecretUpdateGroup safeSecrets = secrets == null
                ? new AdminAiSecretUpdateGroup(null, null)
                : secrets;
        Map<String, AiOpsProviderDefinition> requestProviders = requestPayload.provider().providers();
        Map<String, AiOpsProviderDefinition> baseProviders = base.provider().providers();
        return new AiOpsConfigPayload(
                new AiOpsProviderConfig(
                        requestPayload.provider().activeProvider(),
                        requestPayload.provider().fallbackProvider(),
                        mergeProviders(baseProviders, requestProviders, safeSecrets.providers())
                ),
                requestPayload.resilience(),
                new AiOpsRagConfig(
                        new AiOpsRagAppServerConfig(
                                requestPayload.rag().appServer().baseUrl(),
                                resolveSecret(base.rag().appServer().internalToken(), safeSecrets.appServerInternalToken()),
                                requestPayload.rag().appServer().connectTimeout(),
                                requestPayload.rag().appServer().readTimeout()
                        ),
                        requestPayload.rag().ingestion(),
                        requestPayload.rag().retrieval()
                )
        );
    }

    private Map<String, AiOpsProviderDefinition> sanitizeProviders(Map<String, AiOpsProviderDefinition> providers) {
        Map<String, AiOpsProviderDefinition> sanitized = new LinkedHashMap<>();
        if (providers == null) {
            return sanitized;
        }
        for (Map.Entry<String, AiOpsProviderDefinition> entry : providers.entrySet()) {
            AiOpsProviderDefinition definition = entry.getValue();
            if (definition == null) {
                continue;
            }
            sanitized.put(entry.getKey(), new AiOpsProviderDefinition(
                    new AiOpsChatConfig(
                            definition.chat().baseUrl(),
                            null,
                            definition.chat().model(),
                            definition.chat().timeout(),
                            definition.chat().temperature(),
                            definition.chat().maxTokens()
                    ),
                    new AiOpsEmbeddingConfig(
                            definition.embedding().baseUrl(),
                            null,
                            definition.embedding().model(),
                            definition.embedding().timeout(),
                            definition.embedding().dimension()
                    ),
                    new AiOpsRerankConfig(
                            definition.rerank().baseUrl(),
                            null,
                            definition.rerank().model(),
                            definition.rerank().timeout()
                    )
            ));
        }
        return sanitized;
    }

    private Map<String, AdminAiProviderSecretFieldsVO> buildProviderSecrets(Map<String, AiOpsProviderDefinition> providers) {
        Map<String, AdminAiProviderSecretFieldsVO> result = new LinkedHashMap<>();
        if (providers == null) {
            return result;
        }
        for (Map.Entry<String, AiOpsProviderDefinition> entry : providers.entrySet()) {
            AiOpsProviderDefinition definition = entry.getValue();
            if (definition == null) {
                continue;
            }
            result.put(entry.getKey(), new AdminAiProviderSecretFieldsVO(
                    mask(definition.chat().apiKey()),
                    mask(definition.embedding().apiKey()),
                    mask(definition.rerank().apiKey())
            ));
        }
        return result;
    }

    private Map<String, AiOpsProviderDefinition> mergeProviders(
            Map<String, AiOpsProviderDefinition> baseProviders,
            Map<String, AiOpsProviderDefinition> requestProviders,
            Map<String, AdminAiProviderSecretUpdateGroup> secretProviders
    ) {
        Map<String, AiOpsProviderDefinition> merged = new LinkedHashMap<>();
        if (requestProviders == null) {
            return merged;
        }
        for (Map.Entry<String, AiOpsProviderDefinition> entry : requestProviders.entrySet()) {
            String providerName = entry.getKey();
            AiOpsProviderDefinition requested = entry.getValue();
            if (requested == null) {
                continue;
            }
            AiOpsProviderDefinition existing = baseProviders == null ? null : baseProviders.get(providerName);
            AdminAiProviderSecretUpdateGroup secretGroup = secretProviders == null ? null : secretProviders.get(providerName);
            merged.put(providerName, new AiOpsProviderDefinition(
                    new AiOpsChatConfig(
                            requested.chat().baseUrl(),
                            resolveSecret(existing == null || existing.chat() == null ? null : existing.chat().apiKey(),
                                    secretGroup == null ? null : secretGroup.chatApiKey()),
                            requested.chat().model(),
                            requested.chat().timeout(),
                            requested.chat().temperature(),
                            requested.chat().maxTokens()
                    ),
                    new AiOpsEmbeddingConfig(
                            requested.embedding().baseUrl(),
                            resolveSecret(existing == null || existing.embedding() == null ? null : existing.embedding().apiKey(),
                                    secretGroup == null ? null : secretGroup.embeddingApiKey()),
                            requested.embedding().model(),
                            requested.embedding().timeout(),
                            requested.embedding().dimension()
                    ),
                    new AiOpsRerankConfig(
                            requested.rerank().baseUrl(),
                            resolveSecret(existing == null || existing.rerank() == null ? null : existing.rerank().apiKey(),
                                    secretGroup == null ? null : secretGroup.rerankApiKey()),
                            requested.rerank().model(),
                            requested.rerank().timeout()
                    )
            ));
        }
        return merged;
    }

    private String resolveSecret(String existingValue, AdminAiSecretValueUpdate update) {
        if (update == null || Boolean.TRUE.equals(update.retainExisting())) {
            return existingValue;
        }
        return update.value();
    }

    private AdminOutboxRecordVO toOutboxView(PlatformEventOutboxRecord record) {
        return new AdminOutboxRecordVO(
                record.id(),
                record.eventId(),
                record.eventType(),
                record.routingKey(),
                record.status().name(),
                record.attemptCount(),
                record.traceId(),
                record.lastError(),
                record.nextAttemptAt(),
                record.processingStartedAt(),
                record.publishedAt(),
                record.createdAt(),
                record.updatedAt()
        );
    }
}
