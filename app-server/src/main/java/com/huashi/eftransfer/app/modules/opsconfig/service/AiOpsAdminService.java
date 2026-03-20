package com.huashi.eftransfer.app.modules.opsconfig.service;

import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayCallResult;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayClient;
import com.huashi.eftransfer.app.integration.ai.dto.AiGatewayHealthResponse;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiConfigSaveRequest;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiConfigViewVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretFieldVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretFieldsVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretUpdateGroup;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretValueUpdate;
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
import com.huashi.eftransfer.shared.ai.config.AiOpsRagAppServerConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagIngestionConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagRetrievalConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRerankConfig;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
public class AiOpsAdminService {

    private final AiOpsConfigStorageService storageService;
    private final AiGatewayClient aiGatewayClient;

    public AiOpsAdminService(AiOpsConfigStorageService storageService, AiGatewayClient aiGatewayClient) {
        this.storageService = storageService;
        this.aiGatewayClient = aiGatewayClient;
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
                        new AiOpsChatConfig(
                                payload.provider().chat().baseUrl(),
                                null,
                                payload.provider().chat().model(),
                                payload.provider().chat().timeout(),
                                payload.provider().chat().temperature(),
                                payload.provider().chat().maxTokens()
                        ),
                        new AiOpsEmbeddingConfig(
                                payload.provider().embedding().baseUrl(),
                                null,
                                payload.provider().embedding().model(),
                                payload.provider().embedding().timeout(),
                                payload.provider().embedding().dimension()
                        ),
                        new AiOpsRerankConfig(
                                payload.provider().rerank().baseUrl(),
                                null,
                                payload.provider().rerank().model(),
                                payload.provider().rerank().timeout()
                        )
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
                mask(payload.provider().chat().apiKey()),
                mask(payload.provider().embedding().apiKey()),
                mask(payload.provider().rerank().apiKey()),
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
                ? new AdminAiSecretUpdateGroup(null, null, null, null)
                : secrets;
        return new AiOpsConfigPayload(
                new AiOpsProviderConfig(
                        requestPayload.provider().activeProvider(),
                        requestPayload.provider().fallbackProvider(),
                        new AiOpsChatConfig(
                                requestPayload.provider().chat().baseUrl(),
                                resolveSecret(base.provider().chat().apiKey(), safeSecrets.chatApiKey()),
                                requestPayload.provider().chat().model(),
                                requestPayload.provider().chat().timeout(),
                                requestPayload.provider().chat().temperature(),
                                requestPayload.provider().chat().maxTokens()
                        ),
                        new AiOpsEmbeddingConfig(
                                requestPayload.provider().embedding().baseUrl(),
                                resolveSecret(base.provider().embedding().apiKey(), safeSecrets.embeddingApiKey()),
                                requestPayload.provider().embedding().model(),
                                requestPayload.provider().embedding().timeout(),
                                requestPayload.provider().embedding().dimension()
                        ),
                        new AiOpsRerankConfig(
                                requestPayload.provider().rerank().baseUrl(),
                                resolveSecret(base.provider().rerank().apiKey(), safeSecrets.rerankApiKey()),
                                requestPayload.provider().rerank().model(),
                                requestPayload.provider().rerank().timeout()
                        )
                ),
                requestPayload.resilience(),
                new AiOpsRagConfig(
                        new AiOpsRagAppServerConfig(
                                requestPayload.rag().appServer().baseUrl(),
                                resolveSecret(base.rag().appServer().internalToken(), safeSecrets.appServerInternalToken()),
                                requestPayload.rag().appServer().connectTimeout(),
                                requestPayload.rag().appServer().readTimeout()
                        ),
                        new AiOpsRagIngestionConfig(
                                requestPayload.rag().ingestion().exportPageSize(),
                                requestPayload.rag().ingestion().embeddingBatchSize()
                        ),
                        new AiOpsRagRetrievalConfig(
                                requestPayload.rag().retrieval().recallTopK(),
                                requestPayload.rag().retrieval().recallThreshold(),
                                requestPayload.rag().retrieval().rerankTopN(),
                                requestPayload.rag().retrieval().rerankThreshold(),
                                requestPayload.rag().retrieval().finalTopK()
                        )
                )
        );
    }

    private String resolveSecret(String existingValue, AdminAiSecretValueUpdate update) {
        if (update == null || Boolean.TRUE.equals(update.retainExisting())) {
            return existingValue;
        }
        return update.value();
    }
}
