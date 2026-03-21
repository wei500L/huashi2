package com.huashi.eftransfer.ai.common.runtime;

import com.huashi.eftransfer.ai.common.config.AiProviderProperties;
import com.huashi.eftransfer.ai.common.config.AiResilienceProperties;
import com.huashi.eftransfer.ai.common.observability.ResilientAiExecutor;
import com.huashi.eftransfer.ai.modules.rag.config.RagProperties;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigIssue;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigValidationResponse;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class AiRuntimeConfigService {

    private static final Logger log = LoggerFactory.getLogger(AiRuntimeConfigService.class);
    private static final ParameterizedTypeReference<ApiResponse<AiOpsConfigEffectiveResponse>> EFFECTIVE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final AiProviderProperties providerProperties;
    private final AiResilienceProperties resilienceProperties;
    private final RagProperties ragProperties;
    private final AiRuntimeBundleFactory bundleFactory;
    private final ResilientAiExecutor resilientAiExecutor;
    private final AtomicReference<AiRuntimeBundle> currentBundle = new AtomicReference<>();
    private final AtomicLong versionCounter = new AtomicLong();

    public AiRuntimeConfigService(
            AiProviderProperties providerProperties,
            AiResilienceProperties resilienceProperties,
            RagProperties ragProperties,
            AiRuntimeBundleFactory bundleFactory,
            ResilientAiExecutor resilientAiExecutor
    ) {
        this.providerProperties = providerProperties;
        this.resilienceProperties = resilienceProperties;
        this.ragProperties = ragProperties;
        this.bundleFactory = bundleFactory;
        this.resilientAiExecutor = resilientAiExecutor;
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
        resilientAiExecutor.updateRegistries(bundle.retryRegistry(), bundle.circuitBreakerRegistry());
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
        List<AiOpsConfigIssue> issues = new ArrayList<>();
        List<String> notices = validationNotices(payload);
        if (payload == null) {
            issues.add(new AiOpsConfigIssue("config", "config is required"));
            return new AiOpsConfigValidationResponse(false, issues, notices);
        }
        if (payload.provider() == null) {
            issues.add(new AiOpsConfigIssue("provider", "provider section is required"));
        }
        if (payload.resilience() == null) {
            issues.add(new AiOpsConfigIssue("resilience", "resilience section is required"));
        }
        if (payload.rag() == null) {
            issues.add(new AiOpsConfigIssue("rag", "rag section is required"));
        }
        if (!issues.isEmpty()) {
            return new AiOpsConfigValidationResponse(false, issues, notices);
        }

        validateProvider(payload, issues);
        validateResilience(payload, issues);
        validateRag(payload, issues);

        if (issues.isEmpty()) {
            try {
                bundleFactory.build(payload, "VALIDATION", current().version());
            } catch (RuntimeException ex) {
                issues.add(new AiOpsConfigIssue("config", ex.getMessage()));
            }
        }
        return new AiOpsConfigValidationResponse(issues.isEmpty(), issues, notices);
    }

    public AiOpsConfigEffectiveResponse apply(AiOpsConfigPayload payload, String source, Long version) {
        AiOpsConfigValidationResponse validation = validate(payload);
        if (!validation.valid()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "AI ops config validation failed", 400);
        }
        AiRuntimeBundle bundle = bundleFactory.build(
                payload,
                source,
                version == null ? versionCounter.incrementAndGet() : version
        );
        currentBundle.set(bundle);
        if (version != null) {
            versionCounter.updateAndGet(previous -> Math.max(previous, version));
        }
        resilientAiExecutor.updateRegistries(bundle.retryRegistry(), bundle.circuitBreakerRegistry());
        return effective();
    }

    private void validateProvider(AiOpsConfigPayload payload, List<AiOpsConfigIssue> issues) {
        if (payload.provider().chat() == null) {
            issues.add(new AiOpsConfigIssue("provider.chat", "chat section is required"));
            return;
        }
        if (payload.provider().embedding() == null) {
            issues.add(new AiOpsConfigIssue("provider.embedding", "embedding section is required"));
            return;
        }
        if (payload.provider().rerank() == null) {
            issues.add(new AiOpsConfigIssue("provider.rerank", "rerank section is required"));
            return;
        }
        requireEquals("provider.activeProvider", payload.provider().activeProvider(), "qwen", issues,
                "Only qwen is currently implemented as active provider");
        requireText("provider.fallbackProvider", payload.provider().fallbackProvider(), issues);
        validateUrl("provider.chat.baseUrl", payload.provider().chat().baseUrl(), issues);
        requireText("provider.chat.apiKey", payload.provider().chat().apiKey(), issues);
        requireText("provider.chat.model", payload.provider().chat().model(), issues);
        validateDuration("provider.chat.timeout", payload.provider().chat().timeout(), issues);
        validateRange("provider.chat.temperature", payload.provider().chat().temperature(), 0.0d, 2.0d, issues);
        validatePositive("provider.chat.maxTokens", payload.provider().chat().maxTokens(), issues);

        validateUrl("provider.embedding.baseUrl", payload.provider().embedding().baseUrl(), issues);
        requireText("provider.embedding.apiKey", payload.provider().embedding().apiKey(), issues);
        requireText("provider.embedding.model", payload.provider().embedding().model(), issues);
        validateDuration("provider.embedding.timeout", payload.provider().embedding().timeout(), issues);
        validatePositive("provider.embedding.dimension", payload.provider().embedding().dimension(), issues);
        if (payload.provider().embedding().dimension() != null && payload.provider().embedding().dimension() != 1024) {
            issues.add(new AiOpsConfigIssue(
                    "provider.embedding.dimension",
                    "Current pgvector schema is fixed at 1024 dimensions"
            ));
        }

        validateUrl("provider.rerank.baseUrl", payload.provider().rerank().baseUrl(), issues);
        requireText("provider.rerank.apiKey", payload.provider().rerank().apiKey(), issues);
        requireText("provider.rerank.model", payload.provider().rerank().model(), issues);
        validateDuration("provider.rerank.timeout", payload.provider().rerank().timeout(), issues);
    }

    private void validateResilience(AiOpsConfigPayload payload, List<AiOpsConfigIssue> issues) {
        validatePositive("resilience.maxAttempts", payload.resilience().maxAttempts(), issues);
        validateDuration("resilience.waitDuration", payload.resilience().waitDuration(), issues);
        if (payload.resilience().failureRateThreshold() == null
                || payload.resilience().failureRateThreshold() <= 0
                || payload.resilience().failureRateThreshold() > 100) {
            issues.add(new AiOpsConfigIssue(
                    "resilience.failureRateThreshold",
                    "failureRateThreshold must be between 0 and 100"
            ));
        }
        validatePositive("resilience.slidingWindowSize", payload.resilience().slidingWindowSize(), issues);
        validateDuration("resilience.openStateDuration", payload.resilience().openStateDuration(), issues);
    }

    private void validateRag(AiOpsConfigPayload payload, List<AiOpsConfigIssue> issues) {
        if (payload.rag().appServer() == null) {
            issues.add(new AiOpsConfigIssue("rag.appServer", "appServer section is required"));
            return;
        }
        if (payload.rag().ingestion() == null) {
            issues.add(new AiOpsConfigIssue("rag.ingestion", "ingestion section is required"));
            return;
        }
        if (payload.rag().retrieval() == null) {
            issues.add(new AiOpsConfigIssue("rag.retrieval", "retrieval section is required"));
            return;
        }
        validateUrl("rag.appServer.baseUrl", payload.rag().appServer().baseUrl(), issues);
        requireText("rag.appServer.internalToken", payload.rag().appServer().internalToken(), issues);
        validateDuration("rag.appServer.connectTimeout", payload.rag().appServer().connectTimeout(), issues);
        validateDuration("rag.appServer.readTimeout", payload.rag().appServer().readTimeout(), issues);
        validatePositive("rag.ingestion.exportPageSize", payload.rag().ingestion().exportPageSize(), issues);
        validatePositive("rag.ingestion.embeddingBatchSize", payload.rag().ingestion().embeddingBatchSize(), issues);
        validatePositive("rag.retrieval.recallTopK", payload.rag().retrieval().recallTopK(), issues);
        validateProbability("rag.retrieval.recallThreshold", payload.rag().retrieval().recallThreshold(), issues);
        validatePositive("rag.retrieval.rerankTopN", payload.rag().retrieval().rerankTopN(), issues);
        validateProbability("rag.retrieval.rerankThreshold", payload.rag().retrieval().rerankThreshold(), issues);
        validatePositive("rag.retrieval.finalTopK", payload.rag().retrieval().finalTopK(), issues);
        if (payload.rag().retrieval().rerankTopN() != null
                && payload.rag().retrieval().recallTopK() != null
                && payload.rag().retrieval().rerankTopN() > payload.rag().retrieval().recallTopK()) {
            issues.add(new AiOpsConfigIssue(
                    "rag.retrieval.rerankTopN",
                    "rerankTopN must be less than or equal to recallTopK"
            ));
        }
        if (payload.rag().retrieval().finalTopK() != null
                && payload.rag().retrieval().rerankTopN() != null
                && payload.rag().retrieval().finalTopK() > payload.rag().retrieval().rerankTopN()) {
            issues.add(new AiOpsConfigIssue(
                    "rag.retrieval.finalTopK",
                    "finalTopK must be less than or equal to rerankTopN"
            ));
        }
    }

    private List<String> validationNotices(AiOpsConfigPayload payload) {
        if (payload == null) {
            return List.of();
        }
        return List.of("fallbackProvider is currently informational only; automatic failover is not implemented.");
    }

    private void requireText(String field, String value, List<AiOpsConfigIssue> issues) {
        if (!StringUtils.hasText(value)) {
            issues.add(new AiOpsConfigIssue(field, "value is required"));
        }
    }

    private void requireEquals(String field, String value, String expected, List<AiOpsConfigIssue> issues, String message) {
        if (!expected.equalsIgnoreCase(value == null ? "" : value)) {
            issues.add(new AiOpsConfigIssue(field, message));
        }
    }

    private void validateUrl(String field, String value, List<AiOpsConfigIssue> issues) {
        requireText(field, value, issues);
        if (!StringUtils.hasText(value)) {
            return;
        }
        try {
            URI uri = URI.create(value);
            if (!StringUtils.hasText(uri.getScheme()) || !StringUtils.hasText(uri.getHost())) {
                issues.add(new AiOpsConfigIssue(field, "must be an absolute URL"));
            }
        } catch (Exception ex) {
            issues.add(new AiOpsConfigIssue(field, "must be a valid URL"));
        }
    }

    private void validateDuration(String field, String value, List<AiOpsConfigIssue> issues) {
        requireText(field, value, issues);
        if (!StringUtils.hasText(value)) {
            return;
        }
        try {
            if (Duration.parse(value).isNegative() || Duration.parse(value).isZero()) {
                issues.add(new AiOpsConfigIssue(field, "must be a positive ISO-8601 duration"));
            }
        } catch (Exception ex) {
            issues.add(new AiOpsConfigIssue(field, "must be a valid ISO-8601 duration"));
        }
    }

    private void validatePositive(String field, Integer value, List<AiOpsConfigIssue> issues) {
        if (value == null || value <= 0) {
            issues.add(new AiOpsConfigIssue(field, "must be greater than 0"));
        }
    }

    private void validateProbability(String field, Double value, List<AiOpsConfigIssue> issues) {
        if (value == null || value < 0.0d || value > 1.0d) {
            issues.add(new AiOpsConfigIssue(field, "must be between 0 and 1"));
        }
    }

    private void validateRange(String field, Double value, double min, double max, List<AiOpsConfigIssue> issues) {
        if (value == null || value < min || value > max) {
            issues.add(new AiOpsConfigIssue(field, "must be between " + min + " and " + max));
        }
    }
}
