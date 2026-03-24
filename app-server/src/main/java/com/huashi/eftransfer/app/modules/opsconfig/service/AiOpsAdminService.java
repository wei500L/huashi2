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
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiRuntimeStateVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretFieldVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretFieldsVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretUpdateGroup;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretValueUpdate;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiStoredStateVO;
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
import com.huashi.eftransfer.shared.ai.config.AiOpsRagIngestionConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRagRetrievalConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsRerankConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsResilienceConfig;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        AiOpsConfigEffectiveResponse runtime = aiGatewayClient.fetchEffectiveConfig().orElse(null);
        if (runtime == null && stored.isEmpty()) {
            throw new BusinessException(
                    ResultCode.AI_PROVIDER_UNAVAILABLE,
                    "ai-gateway runtime config is unavailable",
                    503
            );
        }

        AiOpsConfigPayload visiblePayload = runtime != null
                ? normalizePayload(runtime.config())
                : normalizePayload(stored.orElseThrow().config());
        return toView(
                visiblePayload,
                buildNotices(runtime, stored.orElse(null), runtime == null ? List.of() : runtime.notices()),
                runtime,
                stored.orElse(null)
        );
    }

    public AiOpsConfigValidationResponse validate(AdminAiConfigSaveRequest request) {
        AiOpsConfigPayload candidate = mergeSecrets(resolveBaseConfig(), requestPayload(request), request.secrets());
        try {
            return aiGatewayClient.validateConfig(candidate);
        } catch (RuntimeException ex) {
            throw new BusinessException(ResultCode.AI_PROVIDER_UNAVAILABLE, "ai-gateway validation is unavailable", 503);
        }
    }

    @Transactional
    public AdminAiConfigViewVO save(AdminAiConfigSaveRequest request) {
        AdminAiConfigSaveRequest safeRequest = requireRequest(request);
        Optional<StoredAiOpsConfig> storedSnapshot = storageService.load();
        AiOpsConfigEffectiveResponse currentRuntime = aiGatewayClient.fetchEffectiveConfig()
                .orElseThrow(() -> new BusinessException(
                        ResultCode.AI_PROVIDER_UNAVAILABLE,
                        "ai-gateway runtime config is unavailable",
                        503
                ));
        validateExpectedVersion(safeRequest.expectedVersion(), currentVersion(currentRuntime, storedSnapshot.orElse(null)));

        AiOpsConfigPayload candidate = mergeSecrets(
                normalizePayload(currentRuntime.config()),
                requestPayload(safeRequest),
                safeRequest.secrets()
        );

        AiOpsConfigValidationResponse validation;
        try {
            validation = aiGatewayClient.validateConfig(candidate);
        } catch (RuntimeException ex) {
            throw new BusinessException(ResultCode.AI_PROVIDER_UNAVAILABLE, "ai-gateway validation is unavailable", 503);
        }
        if (!validation.valid()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "AI ops config validation failed", 400);
        }

        Long nextVersion = nextVersion(currentRuntime.version(), storedSnapshot.map(StoredAiOpsConfig::version).orElse(null));
        AiOpsConfigEffectiveResponse appliedRuntime;
        try {
            appliedRuntime = applyRuntime(candidate, "DATABASE", nextVersion);
        } catch (RuntimeException ex) {
            throw new BusinessException(ResultCode.AI_PROVIDER_UNAVAILABLE, "ai-gateway apply is unavailable", 503);
        }

        try {
            StoredAiOpsConfig stored = storageService.save(candidate, nextVersion, SecurityUtils.getCurrentUserId().orElse(null));
            return toView(candidate, validation.notices(), appliedRuntime, stored);
        } catch (RuntimeException ex) {
            rollbackRuntime(currentRuntime);
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
            int status = ex.getMessage() != null && ex.getMessage().startsWith("Outbox record was not found")
                    ? 404
                    : 409;
            throw new BusinessException(status == 404 ? ResultCode.NOT_FOUND : ResultCode.BAD_REQUEST, ex.getMessage(), status);
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

    private void rollbackRuntime(AiOpsConfigEffectiveResponse previousRuntime) {
        try {
            applyRuntime(normalizePayload(previousRuntime.config()), previousRuntime.source(), previousRuntime.version());
        } catch (RuntimeException rollbackEx) {
            throw new TransactionSystemException("Failed to rollback ai-gateway runtime config after persistence failure", rollbackEx);
        }
    }

    private AiOpsConfigPayload resolveBaseConfig() {
        return aiGatewayClient.fetchEffectiveConfig()
                .map(AiOpsConfigEffectiveResponse::config)
                .map(this::normalizePayload)
                .orElseGet(() -> storageService.load()
                        .map(StoredAiOpsConfig::config)
                        .map(this::normalizePayload)
                        .orElseThrow(() -> new BusinessException(
                                ResultCode.AI_PROVIDER_UNAVAILABLE,
                                "ai-gateway runtime config is unavailable",
                                503
                        )));
    }

    private AdminAiConfigSaveRequest requireRequest(AdminAiConfigSaveRequest request) {
        if (request == null || request.config() == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "config is required", 400);
        }
        return request;
    }

    private AiOpsConfigPayload requestPayload(AdminAiConfigSaveRequest request) {
        return normalizePayload(requireRequest(request).config());
    }

    private void validateExpectedVersion(Long expectedVersion, Long currentVersion) {
        if (!Objects.equals(expectedVersion, currentVersion)) {
            throw new BusinessException(
                    ResultCode.BAD_REQUEST,
                    "AI ops config was updated by another administrator. Refresh the page and retry.",
                    409
            );
        }
    }

    private Long currentVersion(AiOpsConfigEffectiveResponse runtime, StoredAiOpsConfig stored) {
        if (runtime != null && runtime.version() != null) {
            return runtime.version();
        }
        return stored == null ? null : stored.version();
    }

    private Long nextVersion(Long runtimeVersion, Long storedVersion) {
        long current = Math.max(runtimeVersion == null ? 0L : runtimeVersion, storedVersion == null ? 0L : storedVersion);
        return current + 1L;
    }

    private AiOpsConfigEffectiveResponse applyRuntime(AiOpsConfigPayload payload, String source, Long version) {
        var response = aiGatewayClient.applyConfig(payload, source, version);
        return new AiOpsConfigEffectiveResponse(
                payload,
                response.source(),
                response.version(),
                response.appliedAt(),
                response.notices()
        );
    }

    private List<String> buildNotices(
            AiOpsConfigEffectiveResponse runtime,
            StoredAiOpsConfig stored,
            List<String> runtimeNotices
    ) {
        List<String> notices = new ArrayList<>();
        if (runtimeNotices != null) {
            notices.addAll(runtimeNotices);
        }
        if (runtime == null && stored != null) {
            notices.add("ai-gateway runtime is unavailable. The page is showing the stored database snapshot instead.");
        }
        if (runtime != null && stored != null && !Objects.equals(runtime.version(), stored.version())) {
            notices.add("Stored database config is not in sync with the current ai-gateway runtime version.");
        }
        return notices;
    }

    private AdminAiConfigViewVO toView(
            AiOpsConfigPayload payload,
            List<String> notices,
            AiOpsConfigEffectiveResponse runtime,
            StoredAiOpsConfig stored
    ) {
        AiOpsConfigPayload normalized = normalizePayload(payload);
        String source = runtime != null ? runtime.source() : stored == null ? null : "DATABASE";
        Long version = runtime != null ? runtime.version() : stored == null ? null : stored.version();
        OffsetDateTime updatedAt = runtime != null ? runtime.appliedAt() : stored == null ? null : stored.updatedAt();
        return new AdminAiConfigViewVO(
                sanitize(normalized),
                buildSecrets(normalized),
                source,
                version,
                updatedAt,
                notices == null ? List.of() : notices,
                new AdminAiRuntimeStateVO(
                        runtime != null,
                        runtime == null ? null : runtime.source(),
                        runtime == null ? null : runtime.version(),
                        runtime == null ? null : runtime.appliedAt(),
                        runtime != null && (stored == null || Objects.equals(runtime.version(), stored.version()))
                ),
                new AdminAiStoredStateVO(
                        stored != null,
                        stored == null ? null : stored.version(),
                        stored == null ? null : stored.updatedAt()
                )
        );
    }

    private AiOpsConfigPayload normalizePayload(AiOpsConfigPayload payload) {
        if (payload == null) {
            return new AiOpsConfigPayload(
                    new AiOpsProviderConfig(null, null, Map.of()),
                    null,
                    new AiOpsRagConfig(
                            new AiOpsRagAppServerConfig(null, null, null, null),
                            new AiOpsRagIngestionConfig(null, null),
                            new AiOpsRagRetrievalConfig(null, null, null, null, null)
                    )
            );
        }

        AiOpsProviderConfig provider = payload.provider() == null
                ? new AiOpsProviderConfig(null, null, Map.of())
                : payload.provider();
        Map<String, AiOpsProviderDefinition> providers = new LinkedHashMap<>();
        if (provider.providers() != null) {
            for (Map.Entry<String, AiOpsProviderDefinition> entry : provider.providers().entrySet()) {
                providers.put(entry.getKey(), normalizeProviderDefinition(entry.getValue()));
            }
        }
        AiOpsResilienceConfig resilience = payload.resilience() == null
                ? new AiOpsResilienceConfig(null, null, null, null, null)
                : payload.resilience();

        AiOpsRagConfig rag = payload.rag() == null
                ? new AiOpsRagConfig(null, null, null)
                : payload.rag();
        AiOpsRagAppServerConfig appServer = rag.appServer() == null
                ? new AiOpsRagAppServerConfig(null, null, null, null)
                : rag.appServer();
        AiOpsRagIngestionConfig ingestion = rag.ingestion() == null
                ? new AiOpsRagIngestionConfig(null, null)
                : rag.ingestion();
        AiOpsRagRetrievalConfig retrieval = rag.retrieval() == null
                ? new AiOpsRagRetrievalConfig(null, null, null, null, null)
                : rag.retrieval();

        return new AiOpsConfigPayload(
                new AiOpsProviderConfig(provider.activeProvider(), provider.fallbackProvider(), providers),
                resilience,
                new AiOpsRagConfig(appServer, ingestion, retrieval)
        );
    }

    private AiOpsProviderDefinition normalizeProviderDefinition(AiOpsProviderDefinition definition) {
        if (definition == null) {
            return new AiOpsProviderDefinition(
                    new AiOpsChatConfig(null, null, null, null, null, null),
                    new AiOpsEmbeddingConfig(null, null, null, null, null),
                    new AiOpsRerankConfig(null, null, null, null)
            );
        }
        return new AiOpsProviderDefinition(
                definition.chat() == null ? new AiOpsChatConfig(null, null, null, null, null, null) : definition.chat(),
                definition.embedding() == null ? new AiOpsEmbeddingConfig(null, null, null, null, null) : definition.embedding(),
                definition.rerank() == null ? new AiOpsRerankConfig(null, null, null, null) : definition.rerank()
        );
    }

    private AiOpsConfigPayload sanitize(AiOpsConfigPayload payload) {
        AiOpsConfigPayload normalized = normalizePayload(payload);
        return new AiOpsConfigPayload(
                new AiOpsProviderConfig(
                        normalized.provider().activeProvider(),
                        normalized.provider().fallbackProvider(),
                        sanitizeProviders(normalized.provider().providers())
                ),
                normalized.resilience(),
                new AiOpsRagConfig(
                        new AiOpsRagAppServerConfig(
                                normalized.rag().appServer().baseUrl(),
                                null,
                                normalized.rag().appServer().connectTimeout(),
                                normalized.rag().appServer().readTimeout()
                        ),
                        normalized.rag().ingestion(),
                        normalized.rag().retrieval()
                )
        );
    }

    private AdminAiSecretFieldsVO buildSecrets(AiOpsConfigPayload payload) {
        AiOpsConfigPayload normalized = normalizePayload(payload);
        return new AdminAiSecretFieldsVO(
                buildProviderSecrets(normalized.provider().providers()),
                mask(normalized.rag().appServer().internalToken())
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
        AiOpsConfigPayload normalizedBase = normalizePayload(base);
        AiOpsConfigPayload normalizedRequest = normalizePayload(requestPayload);
        AdminAiSecretUpdateGroup safeSecrets = secrets == null
                ? new AdminAiSecretUpdateGroup(null, null)
                : secrets;
        Map<String, AiOpsProviderDefinition> requestProviders = normalizedRequest.provider().providers();
        Map<String, AiOpsProviderDefinition> baseProviders = normalizedBase.provider().providers();
        return new AiOpsConfigPayload(
                new AiOpsProviderConfig(
                        normalizedRequest.provider().activeProvider(),
                        normalizedRequest.provider().fallbackProvider(),
                        mergeProviders(baseProviders, requestProviders, safeSecrets.providers())
                ),
                normalizedRequest.resilience(),
                new AiOpsRagConfig(
                        new AiOpsRagAppServerConfig(
                                normalizedRequest.rag().appServer().baseUrl(),
                                resolveSecret(normalizedBase.rag().appServer().internalToken(), safeSecrets.appServerInternalToken()),
                                normalizedRequest.rag().appServer().connectTimeout(),
                                normalizedRequest.rag().appServer().readTimeout()
                        ),
                        normalizedRequest.rag().ingestion(),
                        normalizedRequest.rag().retrieval()
                )
        );
    }

    private Map<String, AiOpsProviderDefinition> sanitizeProviders(Map<String, AiOpsProviderDefinition> providers) {
        Map<String, AiOpsProviderDefinition> sanitized = new LinkedHashMap<>();
        if (providers == null) {
            return sanitized;
        }
        for (Map.Entry<String, AiOpsProviderDefinition> entry : providers.entrySet()) {
            AiOpsProviderDefinition definition = normalizeProviderDefinition(entry.getValue());
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
            AiOpsProviderDefinition definition = normalizeProviderDefinition(entry.getValue());
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
            AiOpsProviderDefinition requested = normalizeProviderDefinition(entry.getValue());
            AiOpsProviderDefinition existing = normalizeProviderDefinition(baseProviders == null ? null : baseProviders.get(providerName));
            AdminAiProviderSecretUpdateGroup secretGroup = secretProviders == null ? null : secretProviders.get(providerName);
            merged.put(providerName, new AiOpsProviderDefinition(
                    new AiOpsChatConfig(
                            requested.chat().baseUrl(),
                            resolveSecret(existing.chat().apiKey(),
                                    secretGroup == null ? null : secretGroup.chatApiKey()),
                            requested.chat().model(),
                            requested.chat().timeout(),
                            requested.chat().temperature(),
                            requested.chat().maxTokens()
                    ),
                    new AiOpsEmbeddingConfig(
                            requested.embedding().baseUrl(),
                            resolveSecret(existing.embedding().apiKey(),
                                    secretGroup == null ? null : secretGroup.embeddingApiKey()),
                            requested.embedding().model(),
                            requested.embedding().timeout(),
                            requested.embedding().dimension()
                    ),
                    new AiOpsRerankConfig(
                            requested.rerank().baseUrl(),
                            resolveSecret(existing.rerank().apiKey(),
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
