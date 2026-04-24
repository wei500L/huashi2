package com.huashi.eftransfer.ai.common.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.common.exception.ProviderErrorSupport;
import com.huashi.eftransfer.ai.common.observability.SensitiveDataRedactor;
import com.huashi.eftransfer.shared.ai.config.AiOpsChatConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsEmbeddingConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsProviderDefinition;
import com.huashi.eftransfer.shared.ai.config.AiOpsProtocols;
import com.huashi.eftransfer.shared.ai.config.AiOpsRerankConfig;
import com.huashi.eftransfer.shared.ai.config.AiOpsResilienceConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiCircuitBreakerManagerTest {

    @Test
    void shouldResetCircuitBreakerWhenProviderRuntimeChanges() {
        AiCircuitBreakerManager manager = new AiCircuitBreakerManager(
                new ProviderErrorSupport(new ObjectMapper().findAndRegisterModules(), new SensitiveDataRedactor())
        );

        AiProviderRuntime brokenRuntime = runtime("https://broken-provider.example/v1");
        CircuitBreaker original = manager.circuitBreaker(brokenRuntime, "chat");
        original.transitionToOpenState();

        AiProviderRuntime fixedRuntime = runtime("https://healthy-provider.example/v1");
        CircuitBreaker replacement = manager.circuitBreaker(fixedRuntime, "chat");

        assertThat(replacement).isNotSameAs(original);
        assertThat(original.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(replacement.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void shouldReuseCircuitBreakerWithinSameRuntime() {
        AiCircuitBreakerManager manager = new AiCircuitBreakerManager(
                new ProviderErrorSupport(new ObjectMapper().findAndRegisterModules(), new SensitiveDataRedactor())
        );

        AiProviderRuntime runtime = runtime("https://provider.example/v1");

        CircuitBreaker first = manager.circuitBreaker(runtime, "chat");
        CircuitBreaker second = manager.circuitBreaker(runtime, "chat");

        assertThat(second).isSameAs(first);
    }

    private AiProviderRuntime runtime(String chatBaseUrl) {
        AiOpsProviderDefinition definition = new AiOpsProviderDefinition(
                new AiOpsChatConfig(AiOpsProtocols.OPENAI_COMPAT, chatBaseUrl, "test-api-key", "qwen-max", "PT3S", "PT30S", 0.2d, 1024),
                new AiOpsEmbeddingConfig(AiOpsProtocols.OPENAI_COMPAT, "https://embedding.example/v1", "test-api-key", "text-embedding-v4", "PT3S", "PT30S", 1024),
                new AiOpsRerankConfig(AiOpsProtocols.OPENAI_RERANK, "https://rerank.example", "test-api-key", "gte-rerank-v2", "PT3S", "PT30S")
        );
        return new AiProviderRuntime(
                "qwen",
                definition,
                null,
                null,
                null,
                null,
                new AiOpsResilienceConfig(1, "PT0.1S", 50.0f, 10, "PT5S"),
                mock(RetryRegistry.class),
                null
        );
    }
}
