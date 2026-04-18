package com.huashi.eftransfer.shared.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AiOpsConfigPayload(
        @NotNull(message = "provider section is required")
        @Valid
        AiOpsProviderConfig provider,
        @NotNull(message = "resilience section is required")
        @Valid
        AiOpsResilienceConfig resilience,
        @NotNull(message = "rag section is required")
        @Valid
        AiOpsRagConfig rag
) {
}
