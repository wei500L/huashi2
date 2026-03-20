package com.huashi.eftransfer.ai.common.runtime;

import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;

public record AiRuntimeBundle(
        AiOpsConfigPayload config,
        ChatClient chatClient,
        OpenAiChatModel chatModel,
        EmbeddingModel embeddingModel,
        RestClient rerankRestClient,
        RestClient appServerRestClient,
        RetryRegistry retryRegistry,
        CircuitBreakerRegistry circuitBreakerRegistry,
        String source,
        Long version,
        OffsetDateTime appliedAt
) {
}
