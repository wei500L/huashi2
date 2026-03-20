package com.huashi.eftransfer.ai.common.observability;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ResilientAiExecutor {

    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public ResilientAiExecutor(RetryRegistry retryRegistry, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.retryRegistry = retryRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    public <T> T execute(String operation, Supplier<T> supplier) {
        Retry retry = retryRegistry.retry("ai-" + operation);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("ai-" + operation);

        Supplier<T> guardedSupplier = Retry.decorateSupplier(
                retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, supplier)
        );
        return guardedSupplier.get();
    }
}
