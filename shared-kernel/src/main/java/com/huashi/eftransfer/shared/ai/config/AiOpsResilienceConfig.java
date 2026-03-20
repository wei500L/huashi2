package com.huashi.eftransfer.shared.ai.config;

public record AiOpsResilienceConfig(
        Integer maxAttempts,
        String waitDuration,
        Float failureRateThreshold,
        Integer slidingWindowSize,
        String openStateDuration
) {
}
