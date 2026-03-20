package com.huashi.eftransfer.shared.ai.config;

public record AiOpsConfigPayload(
        AiOpsProviderConfig provider,
        AiOpsResilienceConfig resilience,
        AiOpsRagConfig rag
) {
}
