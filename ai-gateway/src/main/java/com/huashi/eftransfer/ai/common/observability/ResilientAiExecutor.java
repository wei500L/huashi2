package com.huashi.eftransfer.ai.common.observability;

import com.huashi.eftransfer.ai.common.runtime.AiProviderRuntime;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
public class ResilientAiExecutor {

    public <T> T execute(AiProviderRuntime runtime, String operation, Supplier<T> supplier) {
        Retry retry = runtime.retryRegistry().retry(runtime.providerName() + "-" + operation);
        CircuitBreaker circuitBreaker = runtime.circuitBreakerManager()
                .circuitBreaker(runtime.providerName(), operation, runtime.resilienceConfig());

        Supplier<T> guardedSupplier = Retry.decorateSupplier(
                retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, supplier)
        );
        return guardedSupplier.get();
    }
}
