package com.huashi.eftransfer.app.modules.opsconfig.service;

import com.huashi.eftransfer.app.common.audit.service.AuditLogService;
import com.huashi.eftransfer.app.common.outbox.PlatformEventOutboxRecord;
import com.huashi.eftransfer.app.common.outbox.PlatformEventOutboxService;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayCallResult;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayClient;
import com.huashi.eftransfer.shared.ai.AiGatewayHealthResponse;
import com.huashi.eftransfer.shared.ai.AdminAiEmbeddingProbeVO;
import com.huashi.eftransfer.shared.ai.AdminAiRerankProbeVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiConfigDriftVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiConfigSaveRequest;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiConfigViewVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiProviderSecretFieldsVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiProviderSecretUpdateGroup;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiRuntimeSyncRequest;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiRuntimeStateVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretFieldVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretFieldsVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretUpdateGroup;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiSecretValueUpdate;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiStoredStateVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminOutboxRecordVO;
import com.huashi.eftransfer.app.modules.opsconfig.support.AiOpsConfigChangeSet;
import com.huashi.eftransfer.app.modules.opsconfig.support.AiRuntimeSyncOutboxPayload;
import com.huashi.eftransfer.app.modules.opsconfig.support.AiRuntimeSyncOutboxSupport;
import com.huashi.eftransfer.app.modules.opsconfig.support.StoredAiOpsConfig;
import com.huashi.eftransfer.shared.ai.RagReindexJobResponse;
import com.huashi.eftransfer.shared.ai.RagReindexRequest;
import com.huashi.eftransfer.shared.ai.RagReindexResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsChatConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigNotice;
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
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class AiOpsAdminService {

    private final AiOpsConfigStorageService storageService;
    private final AiGatewayClient aiGatewayClient;
    private final PlatformEventOutboxService outboxService;
    private final AuditLogService auditLogService;
    private final AiOpsConfigChangeSummaryService changeSummaryService;
    private final AiOpsLocalValidationService localValidationService;
    private final AiOpsConfigPayloadNormalizer payloadNormalizer;
    private final TransactionTemplate transactionTemplate;

    public AiOpsAdminService(
            AiOpsConfigStorageService storageService,
            AiGatewayClient aiGatewayClient,
            PlatformEventOutboxService outboxService,
            AuditLogService auditLogService,
            AiOpsConfigChangeSummaryService changeSummaryService,
            AiOpsLocalValidationService localValidationService,
            AiOpsConfigPayloadNormalizer payloadNormalizer,
            PlatformTransactionManager transactionManager
    ) {
        this.storageService = storageService;
        this.aiGatewayClient = aiGatewayClient;
        this.outboxService = outboxService;
        this.auditLogService = auditLogService;
        this.changeSummaryService = changeSummaryService;
        this.localValidationService = localValidationService;
        this.payloadNormalizer = payloadNormalizer;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public AdminAiConfigViewVO getCurrentConfig() {
        Optional<StoredAiOpsConfig> stored = storageService.load();
        AiOpsConfigEffectiveResponse runtime = aiGatewayClient.fetchEffectiveConfig().orElse(null);
        StoredAiOpsConfig storedSnapshot = stored.orElse(null);
        AiOpsConfigPayload visiblePayload = authoritativePayloadOrDraft(runtime, storedSnapshot);
        return toView(visiblePayload, runtime == null ? blankDraftNotices(storedSnapshot) : runtime.notices(), runtime, storedSnapshot);
    }

    public AiOpsConfigValidationResponse validate(AdminAiConfigSaveRequest request) {
        AdminAiConfigSaveRequest safeRequest = requireRequest(request);
        AiOpsConfigPayload candidate = mergeSecrets(
                resolveBaseConfig(),
                requestPayload(safeRequest),
                safeRequest.providerOrigins(),
                safeRequest.secrets()
        );
        AiOpsConfigValidationResponse localValidation = localValidationService.validate(candidate, validationNotices());
        if (!localValidation.valid()) {
            return localValidation;
        }
        try {
            return aiGatewayClient.validateConfig(candidate);
        } catch (RuntimeException ex) {
            return new AiOpsConfigValidationResponse(
                    true,
                    List.of(),
                    mergeNotices(validationNotices(), List.of(notice(
                            "runtime_validation_unavailable",
                            "warning",
                            "ai-gateway runtime validation is unavailable. Local schema validation passed; runtime build confirmation is pending."
                    )))
            );
        }
    }

    public AdminAiConfigViewVO save(AdminAiConfigSaveRequest request) {
        AdminAiConfigSaveRequest safeRequest = requireRequest(request);
        Optional<StoredAiOpsConfig> storedSnapshot = storageService.load();
        StoredAiOpsConfig storedConfig = storedSnapshot.orElse(null);
        AiOpsConfigEffectiveResponse currentRuntime = aiGatewayClient.fetchEffectiveConfig().orElse(null);
        validateExpectedVersion(safeRequest.expectedVersion(), currentVersion(currentRuntime, storedConfig));

        AiOpsConfigPayload previousPayload = authoritativePayloadOrDraft(currentRuntime, storedConfig);
        AiOpsConfigPayload candidate = mergeSecrets(
                previousPayload,
                requestPayload(safeRequest),
                safeRequest.providerOrigins(),
                safeRequest.secrets()
        );
        AiOpsConfigChangeSet changeSet = changeSummaryService.summarize(
                previousPayload,
                candidate,
                sanitize(previousPayload),
                sanitize(candidate)
        );
        if (!changeSet.hasChanges()) {
            return toView(
                    previousPayload,
                    currentRuntime == null ? blankDraftNotices(storedConfig) : currentRuntime.notices(),
                    currentRuntime,
                    storedConfig
            );
        }

        localValidationService.requireValid(candidate);

        AiOpsConfigValidationResponse remoteValidation = null;
        List<AiOpsConfigNotice> notices = new ArrayList<>(validationNotices());
        try {
            remoteValidation = aiGatewayClient.validateConfig(candidate);
        } catch (RuntimeException ex) {
            notices.add(notice(
                    "runtime_validation_unavailable",
                    "warning",
                    "ai-gateway runtime validation is unavailable. Local schema validation passed; runtime build confirmation is pending."
            ));
        }
        if (remoteValidation != null) {
            if (!remoteValidation.valid()) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, formatValidationIssues(remoteValidation.issues()), 400);
            }
            notices = mergeNotices(notices, remoteValidation.notices());
        }

        Long previousVersion = currentVersion(currentRuntime, storedConfig);
        Long nextVersion = nextVersion(currentRuntime == null ? null : currentRuntime.version(), storedSnapshot.map(StoredAiOpsConfig::version).orElse(null));
        Long actorUserId = SecurityUtils.getCurrentUserId().orElse(null);
        StoredAiOpsConfig stored = persistStoredConfig(
                candidate,
                safeRequest.expectedVersion(),
                nextVersion,
                previousVersion,
                actorUserId,
                changeSet,
                MDC.get("traceId")
        );
        return toView(candidate, notices, currentRuntime, stored);
    }

    public AiGatewayHealthResponse health() {
        return aiGatewayClient.fetchHealth()
                .orElseThrow(() -> new BusinessException(
                        ResultCode.AI_PROVIDER_UNAVAILABLE,
                        "ai-gateway health is unavailable",
                        503
                ));
    }

    public AdminAiEmbeddingProbeVO probeEmbedding(AdminAiConfigSaveRequest request) {
        AiOpsConfigPayload candidate = buildProbeCandidate(request);
        Map<String, Object> auditPayload = new LinkedHashMap<>();
        auditPayload.put("configKey", AiOpsConfigStorageService.CONFIG_KEY);
        try {
            AiGatewayCallResult<AdminAiEmbeddingProbeVO> result = aiGatewayClient.probeEmbeddingConfig(candidate);
            if (!result.success() || result.data() == null) {
                auditPayload.put("message", result.failureMessage());
                throw new BusinessException(ResultCode.AI_PROVIDER_UNAVAILABLE, result.failureMessage(), 503);
            }
            AdminAiEmbeddingProbeVO probe = result.data();
            auditLogService.record(
                    "ai_ops_embedding_probe",
                    "admin_ai_config",
                    AiOpsConfigStorageService.CONFIG_KEY,
                    buildProbeAuditPayload(probe.provider(), probe.model(), probe.latencyMs(), probe.providerRequestId(), probe.message(), probeDetails(
                            "dimension", probe.dimension(),
                            "expectedDimension", probe.expectedDimension(),
                            "itemCount", probe.itemCount(),
                            "relatedSimilarity", probe.relatedSimilarity(),
                            "unrelatedSimilarity", probe.unrelatedSimilarity(),
                            "similarityMargin", probe.similarityMargin(),
                            "providerCompatibility", probe.providerCompatibility(),
                            "providersChecked", probe.providersChecked()
                    )),
                    probe.ok() ? ResultCode.SUCCESS.code() : ResultCode.AI_PROVIDER_UNAVAILABLE.code()
            );
            return probe;
        } catch (BusinessException exception) {
            recordProbeFailure("ai_ops_embedding_probe", auditPayload, exception);
            throw exception;
        } catch (RuntimeException exception) {
            recordProbeFailure("ai_ops_embedding_probe", auditPayload, exception);
            throw exception;
        }
    }

    public AdminAiRerankProbeVO probeRerank(AdminAiConfigSaveRequest request) {
        AiOpsConfigPayload candidate = buildProbeCandidate(request);
        Map<String, Object> auditPayload = new LinkedHashMap<>();
        auditPayload.put("configKey", AiOpsConfigStorageService.CONFIG_KEY);
        try {
            AiGatewayCallResult<AdminAiRerankProbeVO> result = aiGatewayClient.probeRerankConfig(candidate);
            if (!result.success() || result.data() == null) {
                auditPayload.put("message", result.failureMessage());
                throw new BusinessException(ResultCode.AI_PROVIDER_UNAVAILABLE, result.failureMessage(), 503);
            }
            AdminAiRerankProbeVO probe = result.data();
            auditLogService.record(
                    "ai_ops_rerank_probe",
                    "admin_ai_config",
                    AiOpsConfigStorageService.CONFIG_KEY,
                    buildProbeAuditPayload(probe.provider(), probe.model(), probe.latencyMs(), probe.providerRequestId(), probe.message(), probeDetails(
                            "documentsCount", probe.documentsCount(),
                            "returnedCount", probe.returnedCount(),
                            "ordered", probe.ordered(),
                            "topDocumentIndex", probe.topDocumentIndex(),
                            "topScore", probe.topScore(),
                            "providersChecked", probe.providersChecked()
                    )),
                    probe.ok() ? ResultCode.SUCCESS.code() : ResultCode.AI_PROVIDER_UNAVAILABLE.code()
            );
            return probe;
        } catch (BusinessException exception) {
            recordProbeFailure("ai_ops_rerank_probe", auditPayload, exception);
            throw exception;
        } catch (RuntimeException exception) {
            recordProbeFailure("ai_ops_rerank_probe", auditPayload, exception);
            throw exception;
        }
    }

    public RagReindexResponse triggerReindex(RagReindexRequest request) {
        AiGatewayCallResult<RagReindexResponse> result = aiGatewayClient.reindex(request);
        if (!result.success()) {
            throw new BusinessException(ResultCode.AI_PROVIDER_UNAVAILABLE, result.failureMessage(), 503);
        }
        return result.data();
    }

    public RagReindexJobResponse fetchReindexJob(String jobId) {
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

    public AdminAiConfigViewVO syncRuntime(AdminAiRuntimeSyncRequest request) {
        AdminAiRuntimeSyncRequest safeRequest = request == null
                ? new AdminAiRuntimeSyncRequest(null)
                : request;
        StoredAiOpsConfig stored = storageService.load()
                .orElseThrow(() -> new BusinessException(
                        ResultCode.BAD_REQUEST,
                        "No stored AI ops config is available to sync.",
                        409
                ));
        AiOpsConfigEffectiveResponse currentRuntime = aiGatewayClient.fetchEffectiveConfig().orElse(null);
        validateExpectedVersion(safeRequest.expectedVersion(), currentVersion(currentRuntime, stored));
        AiOpsConfigPayload normalizedStoredConfig = normalizePayload(stored.config());
        PlatformEventOutboxRecord syncRecord = ensureRuntimeSyncOutbox(stored.version(), SecurityUtils.getCurrentUserId().orElse(null), MDC.get("traceId"));
        if (syncRecord.status() != com.huashi.eftransfer.app.common.outbox.PlatformEventOutboxStatus.IN_PROGRESS
                && syncRecord.status() != com.huashi.eftransfer.app.common.outbox.PlatformEventOutboxStatus.PUBLISHED) {
            outboxService.replay(syncRecord.id());
        }
        AiOpsConfigEffectiveResponse runtimeAfterSync = aiGatewayClient.fetchEffectiveConfig().orElse(null);
        return toView(normalizedStoredConfig, validationNotices(), runtimeAfterSync, stored);
    }

    public AdminAiConfigDriftVO getRuntimeDrift() {
        StoredAiOpsConfig stored = storageService.load().orElse(null);
        AiOpsConfigEffectiveResponse runtime = aiGatewayClient.fetchEffectiveConfig().orElse(null);
        PlatformEventOutboxRecord syncRecord = resolveRuntimeSyncRecord(stored);
        return new AdminAiConfigDriftVO(
                new AdminAiRuntimeStateVO(
                        runtime != null,
                        runtime == null ? null : runtime.source(),
                        runtime == null ? null : stringifyVersion(runtime.version()),
                        runtime == null ? null : runtime.appliedAt(),
                        runtime != null && (stored == null || Objects.equals(runtime.version(), stored.version()))
                ),
                new AdminAiStoredStateVO(
                        stored != null,
                        stored == null ? null : stringifyVersion(stored.version()),
                        stored == null ? null : stored.updatedAt()
                ),
                stored != null && (runtime == null || !Objects.equals(runtime.version(), stored.version())),
                syncJobStatus(syncRecord),
                syncRecord == null ? null : syncRecord.attemptCount(),
                syncRecord == null ? null : syncRecord.nextAttemptAt(),
                buildNotices(runtime, stored, runtime == null ? blankDraftNotices(stored) : runtime.notices(), syncRecord)
        );
    }

    public AiOpsConfigEffectiveResponse getStoredConfigForInternalSync() {
        return storageService.load()
                .map(stored -> new AiOpsConfigEffectiveResponse(
                        normalizePayload(stored.config()),
                        "DATABASE",
                        stored.version(),
                        stored.updatedAt(),
                        List.of()
                ))
                .orElse(null);
    }

    private AiOpsConfigPayload resolveBaseConfig() {
        StoredAiOpsConfig stored = storageService.load().orElse(null);
        if (stored != null) {
            return normalizePayload(stored.config());
        }
        return aiGatewayClient.fetchEffectiveConfig()
                .map(AiOpsConfigEffectiveResponse::config)
                .map(this::normalizePayload)
                .orElseGet(this::emptyPayload);
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

    private AiOpsConfigPayload buildProbeCandidate(AdminAiConfigSaveRequest request) {
        AdminAiConfigSaveRequest safeRequest = requireRequest(request);
        AiOpsConfigPayload candidate = mergeSecrets(
                resolveBaseConfig(),
                requestPayload(safeRequest),
                safeRequest.providerOrigins(),
                safeRequest.secrets()
        );
        localValidationService.requireValid(candidate);
        AiOpsConfigValidationResponse validation;
        try {
            validation = aiGatewayClient.validateConfig(candidate);
        } catch (RuntimeException ex) {
            throw new BusinessException(ResultCode.AI_PROVIDER_UNAVAILABLE, "ai-gateway validation is unavailable", 503);
        }
        if (!validation.valid()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "AI ops config validation failed", 400);
        }
        return candidate;
    }

    private void validateExpectedVersion(String expectedVersion, Long currentVersion) {
        if (!Objects.equals(parseVersion(expectedVersion, "expectedVersion"), currentVersion)) {
            throw new BusinessException(
                    ResultCode.BAD_REQUEST,
                    "AI ops config was updated by another administrator. Refresh the page and retry.",
                    409
            );
        }
    }

    private Long currentVersion(AiOpsConfigEffectiveResponse runtime, StoredAiOpsConfig stored) {
        if (stored != null && stored.version() != null) {
            return stored.version();
        }
        if (runtime != null && runtime.version() != null) {
            return runtime.version();
        }
        return null;
    }

    private Long nextVersion(Long runtimeVersion, Long storedVersion) {
        long current = Math.max(runtimeVersion == null ? 0L : runtimeVersion, storedVersion == null ? 0L : storedVersion);
        return current + 1L;
    }

    private StoredAiOpsConfig persistStoredConfig(
            AiOpsConfigPayload candidate,
            String expectedVersion,
            Long nextVersion,
            Long previousVersion,
            Long actorUserId,
            AiOpsConfigChangeSet changeSet,
            String traceId
    ) {
        Long expectedVersionNumber = parseVersion(expectedVersion, "expectedVersion");
        return transactionTemplate.execute(status -> {
            StoredAiOpsConfig stored = storageService.save(candidate, expectedVersionNumber, nextVersion, actorUserId);
            storageService.saveHistory(
                    candidate,
                    nextVersion,
                    previousVersion,
                    actorUserId,
                    buildChangeAuditPayload(changeSet, previousVersion, nextVersion)
            );
            auditLogService.record(
                    "ai_ops_config_save",
                    "admin_ai_config",
                    AiOpsConfigStorageService.CONFIG_KEY,
                    buildSaveAuditPayload(changeSet, previousVersion, nextVersion),
                    ResultCode.SUCCESS.code()
            );
            outboxService.enqueue(
                    AiRuntimeSyncOutboxSupport.eventId(nextVersion),
                    AiRuntimeSyncOutboxSupport.EVENT_TYPE,
                    AiRuntimeSyncOutboxSupport.EXCHANGE_NAME,
                    AiRuntimeSyncOutboxSupport.ROUTING_KEY,
                    new AiRuntimeSyncOutboxPayload(nextVersion, actorUserId, OffsetDateTime.now()),
                    traceId
            );
            return stored;
        });
    }

    private Long parseVersion(String version, String fieldName) {
        if (version == null) {
            return null;
        }
        try {
            return Long.valueOf(version);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldName + " must be a valid integer string", 400);
        }
    }

    private String stringifyVersion(Long version) {
        return version == null ? null : String.valueOf(version);
    }

    private PlatformEventOutboxRecord ensureRuntimeSyncOutbox(Long targetVersion, Long actorUserId, String traceId) {
        PlatformEventOutboxRecord existing = outboxService.findByEventId(AiRuntimeSyncOutboxSupport.eventId(targetVersion));
        if (existing != null) {
            return existing;
        }
        outboxService.enqueue(
                AiRuntimeSyncOutboxSupport.eventId(targetVersion),
                AiRuntimeSyncOutboxSupport.EVENT_TYPE,
                AiRuntimeSyncOutboxSupport.EXCHANGE_NAME,
                AiRuntimeSyncOutboxSupport.ROUTING_KEY,
                new AiRuntimeSyncOutboxPayload(targetVersion, actorUserId, OffsetDateTime.now()),
                traceId
        );
        PlatformEventOutboxRecord created = outboxService.findByEventId(AiRuntimeSyncOutboxSupport.eventId(targetVersion));
        if (created == null) {
            throw new IllegalStateException("Failed to create runtime sync outbox record");
        }
        return created;
    }

    private PlatformEventOutboxRecord resolveRuntimeSyncRecord(StoredAiOpsConfig stored) {
        if (stored == null || stored.version() == null) {
            return null;
        }
        return outboxService.findByEventId(AiRuntimeSyncOutboxSupport.eventId(stored.version()));
    }

    private String syncJobStatus(PlatformEventOutboxRecord syncRecord) {
        if (syncRecord == null) {
            return "NONE";
        }
        return switch (syncRecord.status()) {
            case PENDING, IN_PROGRESS -> "PENDING";
            case FAILED -> "FAILED_RETRYING";
            case DLQ -> "DLQ";
            case PUBLISHED -> "NONE";
        };
    }

    private List<AiOpsConfigNotice> buildNotices(
            AiOpsConfigEffectiveResponse runtime,
            StoredAiOpsConfig stored,
            List<AiOpsConfigNotice> runtimeNotices,
            PlatformEventOutboxRecord syncRecord
    ) {
        List<AiOpsConfigNotice> notices = new ArrayList<>();
        notices = mergeNotices(notices, runtimeNotices);
        if (runtime == null && stored == null) {
            notices = mergeNotices(notices, List.of(notice(
                    "no_stored_snapshot_yet",
                    "warning",
                    "No stored AI ops config exists yet. The page is showing an unsynced draft that can be saved as the first snapshot."
            )));
        }
        if (runtime == null && stored != null) {
            notices = mergeNotices(notices, List.of(notice(
                    "runtime_unavailable_showing_stored",
                    "warning",
                    "ai-gateway runtime is unavailable. The page is showing the stored database snapshot instead."
            )));
        }
        if (stored != null && (runtime == null || !Objects.equals(runtime.version(), stored.version()))) {
            String syncJobStatus = syncJobStatus(syncRecord);
            if ("PENDING".equals(syncJobStatus)) {
                notices = mergeNotices(notices, List.of(notice(
                        "runtime_sync_queued",
                        "warning",
                        "Stored database config is authoritative. Runtime sync has been queued and is waiting to complete."
                )));
            } else if ("FAILED_RETRYING".equals(syncJobStatus)) {
                notices = mergeNotices(notices, List.of(notice(
                        "runtime_sync_retry_scheduled",
                        "warning",
                        "Stored database config is authoritative. Runtime sync previously failed and will be retried automatically."
                )));
            } else if ("DLQ".equals(syncJobStatus)) {
                notices = mergeNotices(notices, List.of(notice(
                        "runtime_sync_dlq",
                        "error",
                        "Stored database config is authoritative, but runtime sync entered the terminal failure queue and requires manual replay."
                )));
            } else {
                notices = mergeNotices(notices, List.of(notice(
                        "stored_runtime_out_of_sync",
                        "warning",
                        "Stored database config is authoritative but not in sync with the current ai-gateway runtime version."
                )));
            }
        }
        return notices;
    }

    private AdminAiConfigViewVO toView(
            AiOpsConfigPayload payload,
            List<AiOpsConfigNotice> notices,
            AiOpsConfigEffectiveResponse runtime,
            StoredAiOpsConfig stored
    ) {
        AiOpsConfigPayload normalized = normalizePayload(payload);
        PlatformEventOutboxRecord syncRecord = resolveRuntimeSyncRecord(stored);
        String source = stored != null ? "DATABASE" : runtime == null ? null : runtime.source();
        String version = stored != null ? stringifyVersion(stored.version()) : runtime == null ? null : stringifyVersion(runtime.version());
        OffsetDateTime updatedAt = stored != null ? stored.updatedAt() : runtime == null ? null : runtime.appliedAt();
        return new AdminAiConfigViewVO(
                sanitize(normalized),
                buildSecrets(normalized),
                source,
                version,
                updatedAt,
                buildNotices(
                        runtime,
                        stored,
                        notices == null ? List.of() : mergeNotices(notices, runtime == null ? List.of() : runtime.notices()),
                        syncRecord
                ),
                new AdminAiRuntimeStateVO(
                        runtime != null,
                        runtime == null ? null : runtime.source(),
                        runtime == null ? null : stringifyVersion(runtime.version()),
                        runtime == null ? null : runtime.appliedAt(),
                        runtime != null && (stored == null || Objects.equals(runtime.version(), stored.version()))
                ),
                new AdminAiStoredStateVO(
                        stored != null,
                        stored == null ? null : stringifyVersion(stored.version()),
                        stored == null ? null : stored.updatedAt()
                )
        );
    }

    private AiOpsConfigPayload authoritativePayloadOrDraft(AiOpsConfigEffectiveResponse runtime, StoredAiOpsConfig stored) {
        if (stored != null) {
            return normalizePayload(stored.config());
        }
        if (runtime != null) {
            return normalizePayload(runtime.config());
        }
        return emptyPayload();
    }

    private AiOpsConfigPayload normalizePayload(AiOpsConfigPayload payload) {
        return payloadNormalizer.normalize(payload);
    }

    private AiOpsProviderDefinition normalizeProviderDefinition(AiOpsProviderDefinition definition) {
        return payloadNormalizer.normalizeProviderDefinition(definition);
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
        return new AdminAiSecretFieldVO(StringUtils.hasText(value), maskValue(value), secretLength(value));
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

    private Integer secretLength(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().length();
    }

    private AiOpsConfigPayload mergeSecrets(
            AiOpsConfigPayload base,
            AiOpsConfigPayload requestPayload,
            Map<String, String> providerOrigins,
            AdminAiSecretUpdateGroup secrets
    ) {
        AiOpsConfigPayload normalizedBase = normalizePayload(base);
        AiOpsConfigPayload normalizedRequest = normalizePayload(requestPayload);
        AdminAiSecretUpdateGroup safeSecrets = secrets == null
                ? new AdminAiSecretUpdateGroup(null, null)
                : secrets;
        Map<String, AiOpsProviderDefinition> requestProviders = normalizedRequest.provider().providers();
        Map<String, AiOpsProviderDefinition> baseProviders = normalizedBase.provider().providers();
        Map<String, String> safeProviderOrigins = validateProviderOrigins(providerOrigins, baseProviders, requestProviders);
        return normalizePayload(
                new AiOpsConfigPayload(
                        new AiOpsProviderConfig(
                                normalizedRequest.provider().activeProvider(),
                                normalizedRequest.provider().fallbackProvider(),
                                mergeProviders(baseProviders, requestProviders, safeProviderOrigins, safeSecrets.providers())
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
                            definition.chat().protocol(),
                            definition.chat().baseUrl(),
                            null,
                            definition.chat().model(),
                            definition.chat().connectTimeout(),
                            definition.chat().readTimeout(),
                            definition.chat().temperature(),
                            definition.chat().maxTokens()
                    ),
                    new AiOpsEmbeddingConfig(
                            definition.embedding().protocol(),
                            definition.embedding().baseUrl(),
                            null,
                            definition.embedding().model(),
                            definition.embedding().multimodalModel(),
                            definition.embedding().connectTimeout(),
                            definition.embedding().readTimeout(),
                            definition.embedding().dimension()
                    ),
                    new AiOpsRerankConfig(
                            definition.rerank().protocol(),
                            definition.rerank().baseUrl(),
                            null,
                            definition.rerank().model(),
                            definition.rerank().multimodalModel(),
                            definition.rerank().connectTimeout(),
                            definition.rerank().readTimeout()
                    )
            ));
        }
        return sanitized;
    }

    private Map<String, AiOpsProviderDefinition> canonicalizeProviders(
            Map<String, AiOpsProviderDefinition> providers,
            String activeProvider,
            String fallbackProvider
    ) {
        Map<String, AiOpsProviderDefinition> ordered = new LinkedHashMap<>();
        addOrderedProvider(ordered, providers, activeProvider);
        addOrderedProvider(ordered, providers, fallbackProvider);
        if (providers != null) {
            providers.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> ordered.putIfAbsent(entry.getKey(), entry.getValue()));
        }
        return ordered;
    }

    private void addOrderedProvider(
            Map<String, AiOpsProviderDefinition> ordered,
            Map<String, AiOpsProviderDefinition> providers,
            String providerName
    ) {
        if (!StringUtils.hasText(providerName) || providers == null || !providers.containsKey(providerName)) {
            return;
        }
        ordered.putIfAbsent(providerName, providers.get(providerName));
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
            Map<String, String> providerOrigins,
            Map<String, AdminAiProviderSecretUpdateGroup> secretProviders
    ) {
        Map<String, AiOpsProviderDefinition> merged = new LinkedHashMap<>();
        if (requestProviders == null) {
            return merged;
        }
        for (Map.Entry<String, AiOpsProviderDefinition> entry : requestProviders.entrySet()) {
            String providerName = entry.getKey();
            String baseLookupName = providerOrigins == null ? providerName : providerOrigins.getOrDefault(providerName, providerName);
            AiOpsProviderDefinition requested = normalizeProviderDefinition(entry.getValue());
            AiOpsProviderDefinition existing = normalizeProviderDefinition(baseProviders == null ? null : baseProviders.get(baseLookupName));
            AdminAiProviderSecretUpdateGroup secretGroup = secretProviders == null ? null : secretProviders.get(providerName);
            merged.put(providerName, new AiOpsProviderDefinition(
                    new AiOpsChatConfig(
                            requested.chat().protocol(),
                            requested.chat().baseUrl(),
                            resolveSecret(existing.chat().apiKey(),
                                    secretGroup == null ? null : secretGroup.chatApiKey()),
                            requested.chat().model(),
                            requested.chat().connectTimeout(),
                            requested.chat().readTimeout(),
                            requested.chat().temperature(),
                            requested.chat().maxTokens()
                    ),
                    new AiOpsEmbeddingConfig(
                            requested.embedding().protocol(),
                            requested.embedding().baseUrl(),
                            resolveSecret(existing.embedding().apiKey(),
                                    secretGroup == null ? null : secretGroup.embeddingApiKey()),
                            requested.embedding().model(),
                            requested.embedding().multimodalModel(),
                            requested.embedding().connectTimeout(),
                            requested.embedding().readTimeout(),
                            requested.embedding().dimension()
                    ),
                    new AiOpsRerankConfig(
                            requested.rerank().protocol(),
                            requested.rerank().baseUrl(),
                            resolveSecret(existing.rerank().apiKey(),
                                    secretGroup == null ? null : secretGroup.rerankApiKey()),
                            requested.rerank().model(),
                            requested.rerank().multimodalModel(),
                            requested.rerank().connectTimeout(),
                            requested.rerank().readTimeout()
                    )
            ));
        }
        return merged;
    }

    private Map<String, String> validateProviderOrigins(
            Map<String, String> providerOrigins,
            Map<String, AiOpsProviderDefinition> baseProviders,
            Map<String, AiOpsProviderDefinition> requestProviders
    ) {
        Map<String, String> normalizedOrigins = new LinkedHashMap<>();
        if (providerOrigins == null || providerOrigins.isEmpty()) {
            return normalizedOrigins;
        }

        Set<String> claimedOrigins = new HashSet<>();
        for (Map.Entry<String, String> entry : providerOrigins.entrySet()) {
            String currentKey = entry.getKey();
            String originKey = entry.getValue();
            if (!StringUtils.hasText(currentKey) || !StringUtils.hasText(originKey)) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "providerOrigins contains blank keys", 400);
            }
            if (requestProviders == null || !requestProviders.containsKey(currentKey)) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "providerOrigins must point to a configured provider", 400);
            }
            if (Objects.equals(currentKey, originKey)) {
                continue;
            }
            if (baseProviders == null || !baseProviders.containsKey(originKey)) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "providerOrigins must reference an existing stored provider", 400);
            }
            if (requestProviders.containsKey(originKey)) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "providerOrigins cannot reuse a source provider that still exists in the request", 400);
            }
            if (!claimedOrigins.add(originKey)) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "providerOrigins cannot reuse the same source provider more than once", 400);
            }
            normalizedOrigins.put(currentKey, originKey);
        }
        return normalizedOrigins;
    }

    private String resolveSecret(String existingValue, AdminAiSecretValueUpdate update) {
        if (update == null || Boolean.TRUE.equals(update.retainExisting())) {
            return existingValue;
        }
        return update.value();
    }

    private AiOpsConfigPayload emptyPayload() {
        return normalizePayload(null);
    }

    private List<AiOpsConfigNotice> validationNotices() {
        return List.of(notice(
                "automatic_failover_enabled",
                "info",
                "Automatic failover is enabled for retryable provider failures and circuit-open scenarios."
        ));
    }

    private List<AiOpsConfigNotice> mergeNotices(List<AiOpsConfigNotice> baseNotices, List<AiOpsConfigNotice> extraNotices) {
        Map<String, AiOpsConfigNotice> notices = new LinkedHashMap<>();
        if (baseNotices != null) {
            baseNotices.forEach(notice -> notices.put(notice.code(), notice));
        }
        if (extraNotices != null) {
            for (AiOpsConfigNotice notice : extraNotices) {
                if (notice != null && StringUtils.hasText(notice.code())) {
                    notices.putIfAbsent(notice.code(), notice);
                }
            }
        }
        return List.copyOf(notices.values());
    }

    private List<AiOpsConfigNotice> blankDraftNotices(StoredAiOpsConfig stored) {
        if (stored != null) {
            return List.of();
        }
        return List.of(notice(
                "no_stored_snapshot_yet",
                "warning",
                "No stored AI ops config exists yet. The page is showing an unsynced draft that can be saved as the first snapshot."
        ));
    }

    private AiOpsConfigNotice notice(String code, String severity, String defaultMessage) {
        return new AiOpsConfigNotice(code, severity, defaultMessage);
    }

    private String formatValidationIssues(List<com.huashi.eftransfer.shared.ai.config.AiOpsConfigIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return "AI ops config validation failed";
        }
        return issues.stream()
                .map(issue -> issue.field() + ": " + issue.message())
                .reduce((left, right) -> left + "; " + right)
                .orElse("AI ops config validation failed");
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

    private Map<String, Object> buildSaveAuditPayload(AiOpsConfigChangeSet changeSet, Long previousVersion, Long nextVersion) {
        Map<String, Object> payload = buildChangeAuditPayload(changeSet, previousVersion, nextVersion);
        payload.put("configKey", AiOpsConfigStorageService.CONFIG_KEY);
        return payload;
    }

    private Map<String, Object> buildChangeAuditPayload(AiOpsConfigChangeSet changeSet, Long previousVersion, Long nextVersion) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("previousVersion", previousVersion);
        payload.put("nextVersion", nextVersion);
        payload.put("configDiffs", changeSet.configDiffs());
        payload.put("secretChanges", changeSet.secretChanges());
        return payload;
    }

    private Map<String, Object> buildProbeAuditPayload(
            String provider,
            String model,
            long latencyMs,
            String providerRequestId,
            String message,
            Map<String, Object> details
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("configKey", AiOpsConfigStorageService.CONFIG_KEY);
        payload.put("provider", provider);
        payload.put("model", model);
        payload.put("latencyMs", latencyMs);
        payload.put("providerRequestId", providerRequestId);
        payload.put("message", message);
        payload.putAll(details);
        return payload;
    }

    private void recordProbeFailure(String actionType, Map<String, Object> basePayload, RuntimeException exception) {
        Map<String, Object> payload = new LinkedHashMap<>(basePayload);
        payload.put("message", exception.getMessage());
        auditLogService.record(
                actionType,
                "admin_ai_config",
                AiOpsConfigStorageService.CONFIG_KEY,
                payload,
                exception instanceof BusinessException businessException
                        ? businessException.getResultCode().code()
                        : ResultCode.INTERNAL_ERROR.code()
        );
    }

    private Map<String, Object> probeDetails(Object... keyValues) {
        Map<String, Object> details = new LinkedHashMap<>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            details.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
        }
        return details;
    }
}
