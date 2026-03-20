package com.huashi.eftransfer.ai.common.observability;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Component
public class ResilientAiExecutor {

    private final AtomicReference<RetryRegistry> retryRegistryRef;
    private final AtomicReference<CircuitBreakerRegistry> circuitBreakerRegistryRef;

    public ResilientAiExecutor(RetryRegistry retryRegistry, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.retryRegistryRef = new AtomicReference<>(retryRegistry);
        this.circuitBreakerRegistryRef = new AtomicReference<>(circuitBreakerRegistry);
    }

    public void updateRegistries(RetryRegistry retryRegistry, CircuitBreakerRegistry circuitBreakerRegistry) {
        retryRegistryRef.set(retryRegistry);
        circuitBreakerRegistryRef.set(circuitBreakerRegistry);
    }

    public <T> T execute(String operation, Supplier<T> supplier) {
        RetryRegistry retryRegistry = retryRegistryRef.get();
        CircuitBreakerRegistry circuitBreakerRegistry = circuitBreakerRegistryRef.get();
        Retry retry = retryRegistry.retry("ai-" + operation);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("ai-" + operation);

        Supplier<T> guardedSupplier = Retry.decorateSupplier(
                retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, supplier)
        );
        return guardedSupplier.get();
    }
}
