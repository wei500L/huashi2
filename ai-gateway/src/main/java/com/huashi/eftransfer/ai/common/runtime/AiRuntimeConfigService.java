package com.huashi.eftransfer.ai.common.runtime;

import com.huashi.eftransfer.ai.common.config.AiProviderProperties;
import com.huashi.eftransfer.ai.common.config.AiResilienceProperties;
import com.huashi.eftransfer.ai.common.observability.SensitiveDataRedactor;
import com.huashi.eftransfer.ai.modules.rag.config.RagProperties;
import com.huashi.eftransfer.ai.modules.rag.service.RagSchemaDimensionGuard;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigApplyResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigIssue;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigNotice;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigSemanticValidator;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigStageResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigValidationResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsChatConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsEmbeddingConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsFlexibleDurationParser;
import com.huashi.eftransfer.shared.ai.config.AiOpsRerankConfig;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AiRuntimeConfigService {

    private static final Logger log = LoggerFactory.getLogger(AiRuntimeConfigService.class);
    private static final ParameterizedTypeReference<ApiResponse<AiOpsConfigEffectiveResponse>> EFFECTIVE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final Duration STAGED_BUNDLE_TTL = Duration.ofMinutes(10);
    private static final int MAX_STAGED_BUNDLES = 16;
    private static final String NOTICE_SEVERITY_INFO = "info";
    private static final String NOTICE_SEVERITY_WARNING = "warning";
    public static final String STORED_SYNC_STATUS_IN_SYNC = "IN_SYNC";
    public static final String STORED_SYNC_STATUS_NO_STORED_CONFIG = "NO_STORED_CONFIG";
    public static final String STORED_SYNC_STATUS_SYNC_FAILED = "SYNC_FAILED";

    private final AiProviderProperties providerProperties;
    private final AiResilienceProperties resilienceProperties;
    private final RagProperties ragProperties;
    private final AiRuntimeBundleFactory bundleFactory;
    private final RagSchemaDimensionGuard ragSchemaDimensionGuard;
    private final SensitiveDataRedactor sensitiveDataRedactor;
    private final Validator validator;
    private final AtomicReference<AiRuntimeBundle> currentBundle = new AtomicReference<>();
    private final AtomicReference<String> storedSyncStatus = new AtomicReference<>(STORED_SYNC_STATUS_NO_STORED_CONFIG);
    private final AtomicBoolean storedConfigSyncInProgress = new AtomicBoolean(false);
    private final AtomicLong versionCounter = new AtomicLong();
    private final ConcurrentMap<String, StagedRuntimeBundle> stagedBundles = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CommittedRuntimeStage> committedStages = new ConcurrentHashMap<>();

    public AiRuntimeConfigService(
            AiProviderProperties providerProperties,
            AiResilienceProperties resilienceProperties,
            RagProperties ragProperties,
            AiRuntimeBundleFactory bundleFactory,
            RagSchemaDimensionGuard ragSchemaDimensionGuard,
            SensitiveDataRedactor sensitiveDataRedactor,
            Validator validator
    ) {
        this.providerProperties = providerProperties;
        this.resilienceProperties = resilienceProperties;
        this.ragProperties = ragProperties;
        this.bundleFactory = bundleFactory;
        this.ragSchemaDimensionGuard = ragSchemaDimensionGuard;
        this.sensitiveDataRedactor = sensitiveDataRedactor;
        this.validator = validator;
    }

    @PostConstruct
    void initialize() {
        AiRuntimeBundle bundle = bundleFactory.fromProperties(
                providerProperties,
                resilienceProperties,
                ragProperties,
                "DEFAULTS",
                versionCounter.incrementAndGet()
        );
        List<AiOpsConfigIssue> bootstrapIssues = collectIssues(bundle.config());
        if (!bootstrapIssues.isEmpty()) {
            throw new IllegalStateException("Invalid bootstrap AI configuration: " + formatIssues(bootstrapIssues));
        }
        ragSchemaDimensionGuard.verifyConfig(bundle.config());
        currentBundle.set(bundle);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncStoredConfigAfterStartup() {
        syncStoredConfig("startup");
    }

    public void retryStoredConfigSyncIfFailed() {
        if (!STORED_SYNC_STATUS_SYNC_FAILED.equals(storedSyncStatus.get())) {
            return;
        }
        syncStoredConfig("health-check");
    }

    @Scheduled(
            initialDelayString = "${ai.runtime.sync-retry-initial-delay:PT10S}",
            fixedDelayString = "${ai.runtime.sync-retry-interval:PT10S}"
    )
    public void retryStoredConfigSyncOnSchedule() {
        if (STORED_SYNC_STATUS_SYNC_FAILED.equals(storedSyncStatus.get())) {
            syncStoredConfig("scheduled-retry");
        }
    }

    private void syncStoredConfig(String trigger) {
        if (!storedConfigSyncInProgress.compareAndSet(false, true)) {
            log.debug("event=ai_runtime_sync_skipped reason=sync_in_progress trigger={}", trigger);
            return;
        }
        try {
            AiRuntimeBundle baseBundle = current();
            ApiResponse<AiOpsConfigEffectiveResponse> response = baseBundle.appServerRestClient().get()
                    .uri("/internal/ops/ai-config")
                    .retrieve()
                    .body(EFFECTIVE_TYPE);

            if (response == null || !response.success() || response.data() == null || response.data().config() == null) {
                storedSyncStatus.set(STORED_SYNC_STATUS_NO_STORED_CONFIG);
                log.info("event=ai_runtime_sync_skipped reason=no_stored_config trigger={}", trigger);
                return;
            }
            apply(response.data().config(), "APP_SERVER_SYNC", response.data().version());
            log.info("event=ai_runtime_sync_applied source=app-server version={} trigger={}",
                    response.data().version(),
                    trigger);
        } catch (RestClientResponseException ex) {
            storedSyncStatus.set(STORED_SYNC_STATUS_SYNC_FAILED);
            log.warn("event=ai_runtime_sync_failed trigger={} status={} body={}",
                    trigger,
                    ex.getStatusCode().value(),
                    sensitiveDataRedactor.redact(ex.getResponseBodyAsString()));
        } catch (RestClientException ex) {
            storedSyncStatus.set(STORED_SYNC_STATUS_SYNC_FAILED);
            log.warn("event=ai_runtime_sync_failed trigger={} message={}", trigger, sensitiveDataRedactor.redact(ex.getMessage()));
        } catch (Exception ex) {
            storedSyncStatus.set(STORED_SYNC_STATUS_SYNC_FAILED);
            log.warn("event=ai_runtime_sync_failed trigger={} message={}", trigger, sensitiveDataRedactor.redact(ex.getMessage()));
        } finally {
            storedConfigSyncInProgress.set(false);
        }
    }

    public AiRuntimeBundle current() {
        return currentBundle.get();
    }

    public AiOpsConfigEffectiveResponse effective() {
        AiRuntimeBundle bundle = current();
        return new AiOpsConfigEffectiveResponse(
                bundle.config(),
                bundle.source(),
                bundle.version(),
                bundle.appliedAt(),
                validationNotices(bundle.config())
        );
    }

    public String storedSyncStatus() {
        return storedSyncStatus.get();
    }

    public AiOpsConfigValidationResponse validate(AiOpsConfigPayload payload) {
        ValidationOutcome outcome = prepareBundle(payload, "VALIDATION", current().version());
        return new AiOpsConfigValidationResponse(
                outcome.issues().isEmpty(),
                outcome.issues(),
                outcome.notices()
        );
    }

    public AiOpsConfigStageResponse stage(AiOpsConfigPayload payload, String source, Long version) {
        pruneStagedEntries();
        Long resolvedVersion = version == null ? versionCounter.incrementAndGet() : version;
        String resolvedSource = StringUtils.hasText(source) ? source : "ADMIN_STAGE";
        ValidationOutcome outcome = prepareBundle(payload, resolvedSource, resolvedVersion);
        if (!outcome.issues().isEmpty() || outcome.bundle() == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, formatIssues(outcome.issues()), 400);
        }

        String stageId = UUID.randomUUID().toString();
        OffsetDateTime stagedAt = OffsetDateTime.now();
        stagedBundles.put(stageId, new StagedRuntimeBundle(
                stageId,
                outcome.bundle(),
                stagedAt,
                stagedAt.plus(STAGED_BUNDLE_TTL)
        ));
        trimStagedEntries();
        return new AiOpsConfigStageResponse(
                stageId,
                outcome.bundle().source(),
                outcome.bundle().version(),
                stagedAt,
                outcome.notices()
        );
    }

    public synchronized AiOpsConfigApplyResponse commit(String stageId) {
        pruneStagedEntries();
        CommittedRuntimeStage committedStage = committedStages.get(stageId);
        if (committedStage != null && !committedStage.isExpired()) {
            return committedStage.response();
        }

        StagedRuntimeBundle stagedBundle = stagedBundles.get(stageId);
        if (stagedBundle == null || stagedBundle.isExpired()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "staged AI ops config was not found or expired", 404);
        }
        rejectIfOlderThanCurrent(stagedBundle.bundle());
        stagedBundles.remove(stageId);

        AiRuntimeBundle activatedBundle = activate(stagedBundle.bundle());
        currentBundle.set(activatedBundle);
        storedSyncStatus.set(STORED_SYNC_STATUS_IN_SYNC);
        if (activatedBundle.version() != null) {
            versionCounter.updateAndGet(previous -> Math.max(previous, activatedBundle.version()));
        }
        AiOpsConfigApplyResponse response = new AiOpsConfigApplyResponse(
                activatedBundle.source(),
                activatedBundle.version(),
                activatedBundle.appliedAt(),
                mergeNotices(validationNotices(activatedBundle.config()), applyNotices(activatedBundle.config()))
        );
        committedStages.put(stageId, new CommittedRuntimeStage(
                response,
                OffsetDateTime.now().plus(STAGED_BUNDLE_TTL)
        ));
        return response;
    }

    public AiOpsConfigEffectiveResponse apply(AiOpsConfigPayload payload, String source, Long version) {
        AiOpsConfigStageResponse staged = stage(payload, source, version);
        AiOpsConfigApplyResponse committed = commit(staged.stageId());
        return new AiOpsConfigEffectiveResponse(
                payload,
                committed.source(),
                committed.version(),
                committed.appliedAt(),
                committed.notices()
        );
    }

    private ValidationOutcome prepareBundle(AiOpsConfigPayload payload, String source, Long version) {
        List<AiOpsConfigIssue> issues = collectIssues(payload);
        List<AiOpsConfigNotice> notices = validationNotices(payload);
        if (!issues.isEmpty()) {
            return new ValidationOutcome(issues, notices, null);
        }
        try {
            AiRuntimeBundle bundle = bundleFactory.build(payload, source, version);
            ragSchemaDimensionGuard.verifyConfig(bundle.config());
            return new ValidationOutcome(
                    List.of(),
                    notices,
                    bundle
            );
        } catch (RuntimeException ex) {
            List<AiOpsConfigIssue> buildIssues = List.of(new AiOpsConfigIssue("config", "runtime_build_failed", ex.getMessage()));
            return new ValidationOutcome(buildIssues, notices, null);
        }
    }

    private List<AiOpsConfigIssue> collectIssues(AiOpsConfigPayload payload) {
        List<AiOpsConfigIssue> issues = new ArrayList<>();
        if (payload != null) {
            for (ConstraintViolation<AiOpsConfigPayload> violation : validator.validate(payload)) {
                issues.add(AiOpsConfigSemanticValidator.issueFromViolation(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ));
            }
        }
        issues.addAll(AiOpsConfigSemanticValidator.validate(payload));

        Map<String, AiOpsConfigIssue> deduped = new LinkedHashMap<>();
        issues.stream()
                .sorted(Comparator.comparing(AiOpsConfigIssue::field).thenComparing(AiOpsConfigIssue::message))
                .forEach(issue -> deduped.putIfAbsent(issue.field() + "\u0000" + issue.message(), issue));
        return List.copyOf(deduped.values());
    }

    private List<AiOpsConfigNotice> validationNotices(AiOpsConfigPayload payload) {
        List<AiOpsConfigNotice> notices = new ArrayList<>();
        notices.add(new AiOpsConfigNotice(
                "automatic_failover_enabled",
                NOTICE_SEVERITY_INFO,
                "Automatic failover is enabled for retryable provider failures and circuit-open scenarios."
        ));
        notices.addAll(fallbackProviderNotices(payload));
        return List.copyOf(notices);
    }

    private List<AiOpsConfigNotice> applyNotices(AiOpsConfigPayload payload) {
        return List.of(new AiOpsConfigNotice(
                "runtime_switch_mixed_window",
                NOTICE_SEVERITY_WARNING,
                "Provider changes can mix old and new bundles for a short window after apply.",
                Map.of("stableWindowSeconds", estimateSwitchWindowSeconds(payload))
        ));
    }

    private List<AiOpsConfigNotice> fallbackProviderNotices(AiOpsConfigPayload payload) {
        if (payload == null || payload.provider() == null || payload.provider().providers() == null) {
            return List.of();
        }
        String activeProvider = payload.provider().activeProvider();
        String fallbackProvider = payload.provider().fallbackProvider();
        if (!StringUtils.hasText(activeProvider) || !StringUtils.hasText(fallbackProvider)) {
            return List.of();
        }
        var providers = payload.provider().providers();
        if (!providers.containsKey(activeProvider) || !providers.containsKey(fallbackProvider)) {
            return List.of();
        }

        var active = providers.get(activeProvider);
        var fallback = providers.get(fallbackProvider);
        if (active == null || fallback == null) {
            return List.of();
        }

        boolean chatSame = sameUpstream(active.chat(), fallback.chat());
        boolean embeddingSame = sameUpstream(active.embedding(), fallback.embedding());
        boolean rerankSame = sameUpstream(active.rerank(), fallback.rerank());

        if (chatSame && embeddingSame && rerankSame) {
            return List.of(new AiOpsConfigNotice(
                    "fallback_same_upstream_all",
                    NOTICE_SEVERITY_WARNING,
                    "Fallback provider resolves to the same upstream chat, embedding, and rerank endpoints as the active provider.",
                    Map.of("activeProvider", activeProvider, "fallbackProvider", fallbackProvider)
            ));
        }

        List<AiOpsConfigNotice> notices = new ArrayList<>();
        if (chatSame) {
            notices.add(new AiOpsConfigNotice(
                    "fallback_same_upstream_chat",
                    NOTICE_SEVERITY_WARNING,
                    "Fallback chat resolves to the same upstream endpoint as the active provider.",
                    Map.of("activeProvider", activeProvider, "fallbackProvider", fallbackProvider)
            ));
        }
        if (embeddingSame) {
            notices.add(new AiOpsConfigNotice(
                    "fallback_same_upstream_embedding",
                    NOTICE_SEVERITY_WARNING,
                    "Fallback embedding resolves to the same upstream endpoint as the active provider.",
                    Map.of("activeProvider", activeProvider, "fallbackProvider", fallbackProvider)
            ));
        }
        if (rerankSame) {
            notices.add(new AiOpsConfigNotice(
                    "fallback_same_upstream_rerank",
                    NOTICE_SEVERITY_WARNING,
                    "Fallback rerank resolves to the same upstream endpoint as the active provider.",
                    Map.of("activeProvider", activeProvider, "fallbackProvider", fallbackProvider)
            ));
        }
        return List.copyOf(notices);
    }

    private boolean sameUpstream(
            AiOpsChatConfig left,
            AiOpsChatConfig right
    ) {
        if (left == null || right == null) {
            return false;
        }
        return sameUpstream(left.protocol(), left.baseUrl(), left.apiKey(), left.model(), null,
                right.protocol(), right.baseUrl(), right.apiKey(), right.model(), null);
    }

    private boolean sameUpstream(
            AiOpsEmbeddingConfig left,
            AiOpsEmbeddingConfig right
    ) {
        if (left == null || right == null) {
            return false;
        }
        return sameUpstream(left.protocol(), left.baseUrl(), left.apiKey(), left.model(), left.multimodalModel(),
                right.protocol(), right.baseUrl(), right.apiKey(), right.model(), right.multimodalModel());
    }

    private boolean sameUpstream(
            AiOpsRerankConfig left,
            AiOpsRerankConfig right
    ) {
        if (left == null || right == null) {
            return false;
        }
        return sameUpstream(left.protocol(), left.baseUrl(), left.apiKey(), left.model(), left.multimodalModel(),
                right.protocol(), right.baseUrl(), right.apiKey(), right.model(), right.multimodalModel());
    }

    private boolean sameUpstream(
            String leftProtocol,
            String leftBaseUrl,
            String leftApiKey,
            String leftModel,
            String leftMultimodalModel,
            String rightProtocol,
            String rightBaseUrl,
            String rightApiKey,
            String rightModel,
            String rightMultimodalModel
    ) {
        return Objects.equals(leftProtocol, rightProtocol)
                && Objects.equals(leftBaseUrl, rightBaseUrl)
                && Objects.equals(leftApiKey, rightApiKey)
                && Objects.equals(leftModel, rightModel)
                && Objects.equals(leftMultimodalModel, rightMultimodalModel);
    }

    private long estimateSwitchWindowSeconds(AiOpsConfigPayload payload) {
        if (payload == null || payload.provider() == null || payload.provider().providers() == null || payload.resilience() == null) {
            return 1L;
        }
        Duration maxTimeout = Duration.ZERO;
        for (var definition : payload.provider().providers().values()) {
            if (definition == null) {
                continue;
            }
            maxTimeout = max(maxTimeout, maxProviderTimeout(definition.chat() == null ? null : definition.chat().connectTimeout(),
                    definition.chat() == null ? null : definition.chat().readTimeout()));
            maxTimeout = max(maxTimeout, maxProviderTimeout(definition.embedding() == null ? null : definition.embedding().connectTimeout(),
                    definition.embedding() == null ? null : definition.embedding().readTimeout()));
            maxTimeout = max(maxTimeout, maxProviderTimeout(definition.rerank() == null ? null : definition.rerank().connectTimeout(),
                    definition.rerank() == null ? null : definition.rerank().readTimeout()));
        }
        int attempts = payload.resilience().maxAttempts() == null ? 1 : Math.max(payload.resilience().maxAttempts(), 1);
        Duration waitDuration = parseNoticeDuration(payload.resilience().waitDuration());
        Duration total = maxTimeout.multipliedBy(attempts).plus(waitDuration.multipliedBy(Math.max(attempts - 1L, 0L)));
        return Math.max(1L, total.toSeconds());
    }

    private Duration parseNoticeDuration(String value) {
        if (!StringUtils.hasText(value)) {
            return Duration.ZERO;
        }
        try {
            return AiOpsFlexibleDurationParser.parse(value);
        } catch (Exception ex) {
            return Duration.ZERO;
        }
    }

    private Duration maxProviderTimeout(String connectTimeout, String readTimeout) {
        return max(parseNoticeDuration(connectTimeout), parseNoticeDuration(readTimeout));
    }

    private Duration max(Duration left, Duration right) {
        if (left == null) {
            return right == null ? Duration.ZERO : right;
        }
        if (right == null) {
            return left;
        }
        return left.compareTo(right) >= 0 ? left : right;
    }

    private String formatIssues(List<AiOpsConfigIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return "AI ops config validation failed";
        }
        return issues.stream()
                .map(issue -> issue.field() + ": " + issue.message())
                .reduce((left, right) -> left + "; " + right)
                .orElse("AI ops config validation failed");
    }

    private AiRuntimeBundle activate(AiRuntimeBundle stagedBundle) {
        return new AiRuntimeBundle(
                stagedBundle.config(),
                stagedBundle.providerRuntimes(),
                stagedBundle.appServerRestClient(),
                stagedBundle.source(),
                stagedBundle.version(),
                OffsetDateTime.now()
        );
    }

    private void rejectIfOlderThanCurrent(AiRuntimeBundle stagedBundle) {
        Long stagedVersion = stagedBundle == null ? null : stagedBundle.version();
        Long currentVersion = current().version();
        if (stagedVersion != null && currentVersion != null && stagedVersion < currentVersion) {
            throw new BusinessException(
                    ResultCode.CONFLICT,
                    "staged AI ops config version is older than the active runtime version",
                    409
            );
        }
    }

    private void pruneStagedEntries() {
        OffsetDateTime now = OffsetDateTime.now();
        stagedBundles.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        committedStages.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    private void trimStagedEntries() {
        if (stagedBundles.size() <= MAX_STAGED_BUNDLES) {
            return;
        }
        stagedBundles.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.comparing(StagedRuntimeBundle::stagedAt)))
                .limit(Math.max(0, stagedBundles.size() - MAX_STAGED_BUNDLES))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(stagedBundles::remove);
    }

    private List<AiOpsConfigNotice> mergeNotices(List<AiOpsConfigNotice> baseNotices, List<AiOpsConfigNotice> extraNotices) {
        Map<String, AiOpsConfigNotice> notices = new LinkedHashMap<>();
        if (baseNotices != null) {
            baseNotices.forEach(notice -> notices.put(notice.code(), notice));
        }
        if (extraNotices != null) {
            extraNotices.forEach(notice -> notices.putIfAbsent(notice.code(), notice));
        }
        return List.copyOf(notices.values());
    }

    private record ValidationOutcome(
            List<AiOpsConfigIssue> issues,
            List<AiOpsConfigNotice> notices,
            AiRuntimeBundle bundle
    ) {
    }

    private record StagedRuntimeBundle(
            String stageId,
            AiRuntimeBundle bundle,
            OffsetDateTime stagedAt,
            OffsetDateTime expiresAt
    ) {
        private boolean isExpired() {
            return expiresAt.isBefore(OffsetDateTime.now());
        }
    }

    private record CommittedRuntimeStage(
            AiOpsConfigApplyResponse response,
            OffsetDateTime expiresAt
    ) {
        private boolean isExpired() {
            return expiresAt.isBefore(OffsetDateTime.now());
        }
    }
}
