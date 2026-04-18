package com.huashi.eftransfer.shared.ai.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AiOpsRagRetrievalConfig(
        @NotNull(message = "recallTopK is required")
        @Positive(message = "recallTopK must be greater than 0")
        Integer recallTopK,
        @NotNull(message = "recallThreshold is required")
        @DecimalMin(value = "0.0", message = "recallThreshold must be between 0 and 1")
        @DecimalMax(value = "1.0", message = "recallThreshold must be between 0 and 1")
        Double recallThreshold,
        @NotNull(message = "rerankTopN is required")
        @Positive(message = "rerankTopN must be greater than 0")
        Integer rerankTopN,
        @NotNull(message = "rerankThreshold is required")
        @DecimalMin(value = "0.0", message = "rerankThreshold must be between 0 and 1")
        @DecimalMax(value = "1.0", message = "rerankThreshold must be between 0 and 1")
        Double rerankThreshold,
        @NotNull(message = "finalTopK is required")
        @Positive(message = "finalTopK must be greater than 0")
        Integer finalTopK,
        @NotNull(message = "hnswEfSearch is required")
        @Positive(message = "hnswEfSearch must be greater than 0")
        Integer hnswEfSearch
) {
}
