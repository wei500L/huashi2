package com.huashi.eftransfer.ai.common.runtime;

import com.huashi.eftransfer.shared.ai.config.AiOpsProviderDefinition;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.client.RestClient;

public record AiProviderRuntime(
        String providerName,
        AiOpsProviderDefinition definition,
        ChatClient chatClient,
        OpenAiChatModel chatModel,
        EmbeddingModel embeddingModel,
        RestClient rerankRestClient,
        RetryRegistry retryRegistry,
        CircuitBreakerRegistry circuitBreakerRegistry
) {
}
