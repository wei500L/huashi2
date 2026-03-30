package com.huashi.eftransfer.ai.common.runtime;

import com.huashi.eftransfer.ai.common.config.AiProviderProperties;
import com.huashi.eftransfer.ai.common.config.AiResilienceProperties;
import com.huashi.eftransfer.ai.modules.rag.config.RagProperties;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigIssue;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigValidationResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsProviderDefinition;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Service
public class AiRuntimeConfigService {

    private static final Logger log = LoggerFactory.getLogger(AiRuntimeConfigService.class);
    private static final Pattern PROVIDER_KEY_PATTERN = Pattern.compile("^[a-z0-9_-]+$");
    private static final ParameterizedTypeReference<ApiResponse<AiOpsConfigEffectiveResponse>> EFFECTIVE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final AiProviderProperties providerProperties;
    private final AiResilienceProperties resilienceProperties;
    private final RagProperties ragProperties;
    private final AiRuntimeBundleFactory bundleFactory;
    private final AtomicReference<AiRuntimeBundle> currentBundle = new AtomicReference<>();
    private final AtomicLong versionCounter = new AtomicLong();

    public AiRuntimeConfigService(
            AiProviderProperties providerProperties,
            AiResilienceProperties resilienceProperties,
            RagProperties ragProperties,
            AiRuntimeBundleFactory bundleFactory
    ) {
        this.providerProperties = providerProperties;
        this.resilienceProperties = resilienceProperties;
        this.ragProperties = ragProperties;
        this.bundleFactory = bundleFactory;
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
        return effective();
    }

    private void validateProvider(AiOpsConfigPayload payload, List<AiOpsConfigIssue> issues) {
        requireText("provider.activeProvider", payload.provider().activeProvider(), issues);
        requireText("provider.fallbackProvider", payload.provider().fallbackProvider(), issues);
        if (payload.provider().providers() == null || payload.provider().providers().isEmpty()) {
            issues.add(new AiOpsConfigIssue("provider.providers", "at least one provider definition is required"));
            return;
        }
        validateProviderReference("provider.activeProvider", payload.provider().activeProvider(), payload.provider().providers(), issues);
        validateProviderReference("provider.fallbackProvider", payload.provider().fallbackProvider(), payload.provider().providers(), issues);
        if (StringUtils.hasText(payload.provider().activeProvider())
                && payload.provider().activeProvider().equalsIgnoreCase(payload.provider().fallbackProvider())) {
            issues.add(new AiOpsConfigIssue("provider.fallbackProvider", "fallbackProvider must be different from activeProvider"));
        }
        for (Map.Entry<String, AiOpsProviderDefinition> entry : payload.provider().providers().entrySet()) {
            validateProviderKey(entry.getKey(), issues);
            validateProviderDefinition(entry.getKey(), entry.getValue(), issues);
        }
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
        return List.of("Automatic failover is enabled for retryable provider failures and circuit-open scenarios.");
    }

    private void requireText(String field, String value, List<AiOpsConfigIssue> issues) {
        if (!StringUtils.hasText(value)) {
            issues.add(new AiOpsConfigIssue(field, "value is required"));
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
            Duration duration = FlexibleDurationParser.parse(value);
            if (duration.isNegative() || duration.isZero()) {
                issues.add(new AiOpsConfigIssue(field, "must be a positive duration"));
            }
        } catch (Exception ex) {
            issues.add(new AiOpsConfigIssue(field, "must be a valid duration"));
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

    private void validateProviderReference(
            String field,
            String providerName,
            Map<String, AiOpsProviderDefinition> providers,
            List<AiOpsConfigIssue> issues
    ) {
        if (!StringUtils.hasText(providerName)) {
            return;
        }
        if (!providers.containsKey(providerName)) {
            issues.add(new AiOpsConfigIssue(field, "must reference a configured provider"));
        }
    }

    private void validateProviderKey(String providerName, List<AiOpsConfigIssue> issues) {
        if (!StringUtils.hasText(providerName)) {
            issues.add(new AiOpsConfigIssue("provider.providers", "provider key must not be blank"));
            return;
        }
        if (!PROVIDER_KEY_PATTERN.matcher(providerName).matches()) {
            issues.add(new AiOpsConfigIssue(
                    "provider.providers." + providerName,
                    "provider key must contain only lowercase letters, numbers, hyphen, or underscore"
            ));
        }
    }

    private void validateProviderDefinition(
            String providerName,
            AiOpsProviderDefinition definition,
            List<AiOpsConfigIssue> issues
    ) {
        String prefix = "provider.providers." + providerName;
        if (definition == null) {
            issues.add(new AiOpsConfigIssue(prefix, "provider definition is required"));
            return;
        }
        if (definition.chat() == null) {
            issues.add(new AiOpsConfigIssue(prefix + ".chat", "chat section is required"));
        } else {
            validateUrl(prefix + ".chat.baseUrl", definition.chat().baseUrl(), issues);
            requireText(prefix + ".chat.apiKey", definition.chat().apiKey(), issues);
            requireText(prefix + ".chat.model", definition.chat().model(), issues);
            validateDuration(prefix + ".chat.timeout", definition.chat().timeout(), issues);
            validateRange(prefix + ".chat.temperature", definition.chat().temperature(), 0.0d, 2.0d, issues);
            validatePositive(prefix + ".chat.maxTokens", definition.chat().maxTokens(), issues);
        }
        if (definition.embedding() == null) {
            issues.add(new AiOpsConfigIssue(prefix + ".embedding", "embedding section is required"));
        } else {
            validateUrl(prefix + ".embedding.baseUrl", definition.embedding().baseUrl(), issues);
            requireText(prefix + ".embedding.apiKey", definition.embedding().apiKey(), issues);
            requireText(prefix + ".embedding.model", definition.embedding().model(), issues);
            validateDuration(prefix + ".embedding.timeout", definition.embedding().timeout(), issues);
            validatePositive(prefix + ".embedding.dimension", definition.embedding().dimension(), issues);
            if (definition.embedding().dimension() != null && definition.embedding().dimension() != 1024) {
                issues.add(new AiOpsConfigIssue(
                        prefix + ".embedding.dimension",
                        "Current pgvector schema is fixed at 1024 dimensions"
                ));
            }
        }
        if (definition.rerank() == null) {
            issues.add(new AiOpsConfigIssue(prefix + ".rerank", "rerank section is required"));
        } else {
            validateUrl(prefix + ".rerank.baseUrl", definition.rerank().baseUrl(), issues);
            requireText(prefix + ".rerank.apiKey", definition.rerank().apiKey(), issues);
            requireText(prefix + ".rerank.model", definition.rerank().model(), issues);
            validateDuration(prefix + ".rerank.timeout", definition.rerank().timeout(), issues);
        }
    }
}
