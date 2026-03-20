package com.huashi.eftransfer.ai.common.observability;

import com.huashi.eftransfer.ai.common.exception.ProviderCallException;
import com.huashi.eftransfer.ai.common.exception.ProviderErrorSupport;
import com.huashi.eftransfer.shared.ai.TokenUsage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class AiProviderObservationService {

    private static final Logger log = LoggerFactory.getLogger(AiProviderObservationService.class);

    private final MeterRegistry meterRegistry;
    private final ProviderErrorSupport providerErrorSupport;
    private final ProviderRequestContextHolder providerRequestContextHolder;

    public AiProviderObservationService(
            MeterRegistry meterRegistry,
            ProviderErrorSupport providerErrorSupport,
            ProviderRequestContextHolder providerRequestContextHolder
    ) {
        this.meterRegistry = meterRegistry;
        this.providerErrorSupport = providerErrorSupport;
        this.providerRequestContextHolder = providerRequestContextHolder;
    }

    public void recordSuccess(
            String operation,
            String provider,
            String model,
            long startNanos,
            String providerRequestId,
            TokenUsage usage
    ) {
        long elapsed = System.nanoTime() - startNanos;
        recordMetrics(operation, provider, model, "success", elapsed);
        recordUsage(operation, provider, model, usage);
        log.info(
                "event=ai_provider_call operation={} provider={} model={} latencyMs={} providerRequestId={} promptTokens={} completionTokens={} totalTokens={} outcome=success",
                operation,
                provider,
                model,
                TimeUnit.NANOSECONDS.toMillis(elapsed),
                providerRequestId,
                usage != null ? usage.promptTokens() : null,
                usage != null ? usage.completionTokens() : null,
                usage != null ? usage.totalTokens() : null
        );
        providerRequestContextHolder.clear();
    }

    public ProviderCallException recordFailure(
            String operation,
            String provider,
            String model,
            long startNanos,
            Throwable throwable
    ) {
        ProviderCallException exception = providerErrorSupport.map(
                throwable,
                operation,
                provider,
                model,
                providerRequestContextHolder.getRequestId()
        );
        long elapsed = System.nanoTime() - startNanos;
        recordMetrics(operation, provider, model, exception.getOutcome(), elapsed);
        log.warn(
                "event=ai_provider_call operation={} provider={} model={} latencyMs={} providerRequestId={} providerStatus={} providerCode={} outcome={} retryable={} reason={}",
                operation,
                provider,
                model,
                TimeUnit.NANOSECONDS.toMillis(elapsed),
                exception.getProviderRequestId(),
                exception.getProviderStatus(),
                exception.getProviderCode(),
                exception.getOutcome(),
                exception.isRetryable(),
                exception.getMessage()
        );
        providerRequestContextHolder.clear();
        return exception;
    }

    private void recordMetrics(String operation, String provider, String model, String outcome, long elapsedNanos) {
        Counter.builder("ai.provider.calls")
                .tag("operation", operation)
                .tag("provider", provider)
                .tag("model", model)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();

        Timer.builder("ai.provider.latency")
                .tag("operation", operation)
                .tag("provider", provider)
                .tag("model", model)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(elapsedNanos, TimeUnit.NANOSECONDS);
    }

    private void recordUsage(String operation, String provider, String model, TokenUsage usage) {
        if (usage == null) {
            return;
        }
        incrementTokenCounter(operation, provider, model, "prompt", usage.promptTokens());
        incrementTokenCounter(operation, provider, model, "completion", usage.completionTokens());
        incrementTokenCounter(operation, provider, model, "total", usage.totalTokens());
    }

    private void incrementTokenCounter(String operation, String provider, String model, String type, Integer value) {
        if (value == null) {
            return;
        }
        Counter.builder("ai.provider.tokens")
                .tag("operation", operation)
                .tag("provider", provider)
                .tag("model", model)
                .tag("type", type)
                .register(meterRegistry)
                .increment(value);
    }
}
