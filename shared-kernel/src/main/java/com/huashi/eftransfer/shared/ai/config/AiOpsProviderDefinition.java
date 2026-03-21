package com.huashi.eftransfer.shared.ai.config;

public record AiOpsProviderDefinition(
        AiOpsChatConfig chat,
        AiOpsEmbeddingConfig embedding,
        AiOpsRerankConfig rerank
) {
}
