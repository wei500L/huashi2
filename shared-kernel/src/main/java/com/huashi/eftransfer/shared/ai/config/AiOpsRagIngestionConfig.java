package com.huashi.eftransfer.shared.ai.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AiOpsRagIngestionConfig(
        @NotNull(message = "exportPageSize is required")
        @Positive(message = "exportPageSize must be greater than 0")
        Integer exportPageSize,
        @NotNull(message = "embeddingBatchSize is required")
        @Positive(message = "embeddingBatchSize must be greater than 0")
        Integer embeddingBatchSize
) {
}
