package com.huashi.eftransfer.ai.common.runtime;

import com.huashi.eftransfer.ai.common.config.AiProviderProperties;
import com.huashi.eftransfer.ai.common.config.AiResilienceProperties;
import com.huashi.eftransfer.ai.modules.rag.config.RagProperties;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigApplyResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigIssue;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigSemanticValidator;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigStageResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigValidationResponse;
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
import java.util.UUID;
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

    private final AiProviderProperties providerProperties;
    private final AiResilienceProperties resilienceProperties;
    private final RagProperties ragProperties;
    private final AiRuntimeBundleFactory bundleFactory;
    private final Validator validator;
    private final AtomicReference<AiRuntimeBundle> currentBundle = new AtomicReference<>();
    private final AtomicLong versionCounter = new AtomicLong();
    private final ConcurrentMap<String, StagedRuntimeBundle> stagedBundles = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CommittedRuntimeStage> committedStages = new ConcurrentHashMap<>();

    public AiRuntimeConfigService(
            AiProviderProperties providerProperties,
            AiResilienceProperties resilienceProperties,
            RagProperties ragProperties,
            AiRuntimeBundleFactory bundleFactory,
            Validator validator
    ) {
        this.providerProperties = providerProperties;
        this.resilienceProperties = resilienceProperties;
        this.ragProperties = ragProperties;
        this.bundleFactory = bundleFactory;
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
        currentBundle.set(bundle);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncStoredConfigAfterStartup() {
        try {
            AiRuntimeBundle baseBundle = current();
            ApiResponse<AiOpsConfigEffectiveResponse> response = baseBundle.appServerRestClient().get()
                    .uri("/internal/ops/ai-config")
                    .retrieve()
                    .body(EFFECTIVE_TYPE);

            if (response == null || !response.success() || response.data() == null || response.data().config() == null) {
                log.info("event=ai_runtime_sync_skipped reason=no_stored_config");
                return;
            }
            apply(response.data().config(), "APP_SERVER_SYNC", response.data().version());
            log.info("event=ai_runtime_sync_applied source=app-server version={}", response.data().version());
        } catch (RestClientResponseException ex) {
            log.warn("event=ai_runtime_sync_failed status={} message={}", ex.getStatusCode().value(), ex.getMessage());
        } catch (RestClientException ex) {
            log.warn("event=ai_runtime_sync_failed message={}", ex.getMessage());
        } catch (Exception ex) {
            log.warn("event=ai_runtime_sync_failed message={}", ex.getMessage());
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

    public AiOpsConfigApplyResponse commit(String stageId) {
        pruneStagedEntries();
        CommittedRuntimeStage committedStage = committedStages.get(stageId);
        if (committedStage != null && !committedStage.isExpired()) {
            return committedStage.response();
        }

        StagedRuntimeBundle stagedBundle = stagedBundles.remove(stageId);
        if (stagedBundle == null || stagedBundle.isExpired()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "staged AI ops config was not found or expired", 404);
        }

        AiRuntimeBundle activatedBundle = activate(stagedBundle.bundle());
        currentBundle.set(activatedBundle);
        if (activatedBundle.version() != null) {
            versionCounter.updateAndGet(previous -> Math.max(previous, activatedBundle.version()));
        }
        AiOpsConfigApplyResponse response = new AiOpsConfigApplyResponse(
                activatedBundle.source(),
                activatedBundle.version(),
                activatedBundle.appliedAt(),
                validationNotices(activatedBundle.config())
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
        List<String> notices = validationNotices(payload);
        if (!issues.isEmpty()) {
            return new ValidationOutcome(issues, notices, null);
        }
        try {
            return new ValidationOutcome(
                    List.of(),
                    notices,
                    bundleFactory.build(payload, source, version)
            );
        } catch (RuntimeException ex) {
            List<AiOpsConfigIssue> buildIssues = List.of(new AiOpsConfigIssue("config", ex.getMessage()));
            return new ValidationOutcome(buildIssues, notices, null);
        }
    }

    private List<AiOpsConfigIssue> collectIssues(AiOpsConfigPayload payload) {
        List<AiOpsConfigIssue> issues = new ArrayList<>();
        if (payload != null) {
            for (ConstraintViolation<AiOpsConfigPayload> violation : validator.validate(payload)) {
                issues.add(new AiOpsConfigIssue(
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

    private List<String> validationNotices(AiOpsConfigPayload payload) {
        return List.of("Automatic failover is enabled for retryable provider failures and circuit-open scenarios.");
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

    private record ValidationOutcome(
            List<AiOpsConfigIssue> issues,
            List<String> notices,
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
