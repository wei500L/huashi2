package com.huashi.eftransfer.shared.ai.config;

public record AiOpsChatConfig(
        String baseUrl,
        String apiKey,
        String model,
        String timeout,
        Double temperature,
        Integer maxTokens
) {
}
