package com.huashi.eftransfer.shared.ai.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;

public record AiOpsRagIngestionConfig(
        @NotNull(message = "exportPageSize is required")
        @Positive(message = "exportPageSize must be greater than 0")
        Integer exportPageSize,
        @NotNull(message = "embeddingBatchSize is required")
        @Positive(message = "embeddingBatchSize must be greater than 0")
        @Max(value = 128, message = "embeddingBatchSize must be less than or equal to 128")
        Integer embeddingBatchSize,
        Boolean failedRetryEnabled,
        @Positive(message = "failedRetryLimit must be greater than 0")
        @Max(value = 256, message = "failedRetryLimit must be less than or equal to 256")
        Integer failedRetryLimit
) {
    public AiOpsRagIngestionConfig(Integer exportPageSize, Integer embeddingBatchSize) {
        this(exportPageSize, embeddingBatchSize, Boolean.TRUE, 64);
    }

    @JsonCreator
    public AiOpsRagIngestionConfig(
            @JsonProperty("exportPageSize") Integer exportPageSize,
            @JsonProperty("embeddingBatchSize") Integer embeddingBatchSize,
            @JsonProperty("failedRetryEnabled") Boolean failedRetryEnabled,
            @JsonProperty("failedRetryLimit") Integer failedRetryLimit,
            @JsonProperty("batchSize") Integer legacyBatchSize,
            @JsonProperty("maxConcurrency") Integer legacyMaxConcurrency
    ) {
        this(
                exportPageSize != null ? exportPageSize : legacyBatchSize,
                embeddingBatchSize != null ? embeddingBatchSize : legacyMaxConcurrency,
                failedRetryEnabled == null ? Boolean.TRUE : failedRetryEnabled,
                failedRetryLimit == null ? 64 : failedRetryLimit
        );
    }

    public boolean resolveFailedRetryEnabled() {
        return failedRetryEnabled == null || failedRetryEnabled;
    }

    public int resolvedFailedRetryLimit() {
        return failedRetryLimit == null ? 64 : failedRetryLimit;
    }
}
