package com.huashi.eftransfer.ai.common.runtime;

import com.huashi.eftransfer.ai.common.exception.ProviderErrorSupport;
import com.huashi.eftransfer.shared.ai.config.AiOpsFlexibleDurationParser;
import com.huashi.eftransfer.shared.ai.config.AiOpsResilienceConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AiCircuitBreakerManager {

    private final ProviderErrorSupport providerErrorSupport;
    private final ConcurrentMap<String, CachedCircuitBreaker> breakers = new ConcurrentHashMap<>();

    public AiCircuitBreakerManager(ProviderErrorSupport providerErrorSupport) {
        this.providerErrorSupport = providerErrorSupport;
    }

    public CircuitBreaker circuitBreaker(String providerName, String operation, AiOpsResilienceConfig resilienceConfig) {
        String key = providerName + "-" + operation;
        CircuitBreakerSpec spec = CircuitBreakerSpec.from(resilienceConfig);
        CachedCircuitBreaker cached = breakers.compute(key, (ignored, existing) -> {
            if (existing != null && existing.spec().equals(spec)) {
                return existing;
            }
            return new CachedCircuitBreaker(
                    spec,
                    CircuitBreaker.of(key, CircuitBreakerConfig.custom()
                            .failureRateThreshold(spec.failureRateThreshold())
                            .slidingWindowSize(spec.slidingWindowSize())
                            .minimumNumberOfCalls(spec.slidingWindowSize())
                            .waitDurationInOpenState(spec.openStateDuration())
                            .recordException(providerErrorSupport::shouldRecordForCircuitBreaker)
                            .build())
            );
        });
        return cached.circuitBreaker();
    }

    private record CachedCircuitBreaker(
            CircuitBreakerSpec spec,
            CircuitBreaker circuitBreaker
    ) {
    }

    private record CircuitBreakerSpec(
            float failureRateThreshold,
            int slidingWindowSize,
            Duration openStateDuration
    ) {
        private static CircuitBreakerSpec from(AiOpsResilienceConfig config) {
            return new CircuitBreakerSpec(
                    config == null || config.failureRateThreshold() == null ? 50F : config.failureRateThreshold(),
                    config == null || config.slidingWindowSize() == null ? 20 : config.slidingWindowSize(),
                    parseDuration(config == null ? null : config.openStateDuration())
            );
        }

        private static Duration parseDuration(String value) {
            Duration duration = AiOpsFlexibleDurationParser.parse(value);
            if (duration == null || duration.isZero() || duration.isNegative()) {
                return Duration.ofSeconds(30);
            }
            return duration;
        }
    }
}
