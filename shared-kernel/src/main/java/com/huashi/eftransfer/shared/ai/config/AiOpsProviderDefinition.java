package com.huashi.eftransfer.shared.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AiOpsProviderDefinition(
        @NotNull(message = "chat section is required")
        @Valid
        AiOpsChatConfig chat,
        @NotNull(message = "embedding section is required")
        @Valid
        AiOpsEmbeddingConfig embedding,
        @NotNull(message = "rerank section is required")
        @Valid
        AiOpsRerankConfig rerank
) {
}
