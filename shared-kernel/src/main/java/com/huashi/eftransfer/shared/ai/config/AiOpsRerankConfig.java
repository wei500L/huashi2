package com.huashi.eftransfer.shared.ai.config;

public record AiOpsRerankConfig(
        String baseUrl,
        String apiKey,
        String model,
        String timeout
) {
}
