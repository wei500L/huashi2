package com.huashi.eftransfer.ai.integration.provider;

import com.huashi.eftransfer.ai.common.exception.ProviderCallException;
import com.huashi.eftransfer.ai.common.runtime.AiProviderRuntime;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeBundle;
import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.shared.ai.ChatRequest;
import com.huashi.eftransfer.shared.ai.ChatResponse;
import com.huashi.eftransfer.shared.ai.EmbeddingBatchRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AiProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(AiProviderRegistry.class);

    private final AiRuntimeConfigService runtimeConfigService;
    private final QwenChatProviderClient chatProviderClient;
    private final QwenEmbeddingProviderClient embeddingProviderClient;
    private final QwenRerankClient rerankClient;

    public AiProviderRegistry(
            AiRuntimeConfigService runtimeConfigService,
            QwenChatProviderClient chatProviderClient,
            QwenEmbeddingProviderClient embeddingProviderClient,
            QwenRerankClient rerankClient
    ) {
        this.runtimeConfigService = runtimeConfigService;
        this.chatProviderClient = chatProviderClient;
        this.embeddingProviderClient = embeddingProviderClient;
        this.rerankClient = rerankClient;
    }

    public ChatResponse chat(ChatRequest request) {
        return executeWithFailover(
                "chat",
                (runtime, providerName) -> chatProviderClient.chat(runtime, providerName, request)
        );
    }

    public StructuredChatResponse structuredChat(StructuredChatRequest request) {
        return executeWithFailover(
                "structured-chat",
                (runtime, providerName) -> chatProviderClient.structuredChat(runtime, providerName, request)
        );
    }

    public EmbeddingResponse embed(EmbeddingRequest request) {
        return executeWithFailover(
                "embedding",
                (runtime, providerName) -> embeddingProviderClient.embed(runtime, providerName, request)
        );
    }

    public EmbeddingResponse embedBatch(EmbeddingBatchRequest request) {
        return executeWithFailover(
                "embedding-batch",
                (runtime, providerName) -> embeddingProviderClient.embedBatch(runtime, providerName, request)
        );
    }

    public RerankResponse rerank(RerankRequest request) {
        return executeWithFailover(
                "rerank",
                (runtime, providerName) -> rerankClient.rerank(runtime, providerName, request)
        );
    }

    public AiProviderRuntime activeRuntime() {
        AiRuntimeBundle bundle = runtimeConfigService.current();
        return requireRuntime(bundle, bundle.config().provider().activeProvider());
    }

    private <T> T executeWithFailover(String operation, ProviderOperation<T> providerOperation) {
        AiRuntimeBundle bundle = runtimeConfigService.current();
        String activeProvider = bundle.config().provider().activeProvider();
        String fallbackProvider = bundle.config().provider().fallbackProvider();
        AiProviderRuntime activeRuntime = requireRuntime(bundle, activeProvider);
        try {
            return providerOperation.execute(activeRuntime, activeProvider);
        } catch (ProviderCallException ex) {
            if (!shouldFailover(bundle, activeProvider, fallbackProvider, ex)) {
                throw ex;
            }
            log.warn("event=ai_provider_failover operation={} fromProvider={} toProvider={} reason={}",
                    operation, activeProvider, fallbackProvider, ex.getMessage());
            AiProviderRuntime fallbackRuntime = requireRuntime(bundle, fallbackProvider);
            return providerOperation.execute(fallbackRuntime, fallbackProvider);
        }
    }

    private boolean shouldFailover(
            AiRuntimeBundle bundle,
            String activeProvider,
            String fallbackProvider,
            ProviderCallException exception
    ) {
        return StringUtils.hasText(fallbackProvider)
                && !fallbackProvider.equalsIgnoreCase(activeProvider)
                && bundle.providerRuntime(fallbackProvider) != null
                && (exception.isRetryable() || "circuit_open".equals(exception.getOutcome()));
    }

    private AiProviderRuntime requireRuntime(AiRuntimeBundle bundle, String providerName) {
        AiProviderRuntime runtime = bundle.providerRuntime(providerName);
        if (runtime == null) {
            throw new BusinessException(
                    ResultCode.AI_PROVIDER_UNAVAILABLE,
                    "No configured AI provider runtime found for " + providerName,
                    503
            );
        }
        return runtime;
    }

    @FunctionalInterface
    private interface ProviderOperation<T> {
        T execute(AiProviderRuntime runtime, String providerName);
    }
}
