package com.huashi.eftransfer.shared.ai.config;

public record AiOpsEmbeddingConfig(
        String baseUrl,
        String apiKey,
        String model,
        String timeout,
        Integer dimension
) {
}
