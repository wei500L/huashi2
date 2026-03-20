package com.huashi.eftransfer.shared.ai.config;

public record AiOpsProviderConfig(
        String activeProvider,
        String fallbackProvider,
        AiOpsChatConfig chat,
        AiOpsEmbeddingConfig embedding,
        AiOpsRerankConfig rerank
) {
}
