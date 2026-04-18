package com.huashi.eftransfer.shared.ai.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AiOpsEmbeddingConfig(
        @NotBlank(message = "protocol is required")
        String protocol,
        @NotBlank(message = "baseUrl is required")
        String baseUrl,
        String apiKey,
        @NotBlank(message = "model is required")
        String model,
        @NotBlank(message = "connectTimeout is required")
        String connectTimeout,
        @NotBlank(message = "readTimeout is required")
        String readTimeout,
        @NotNull(message = "dimension is required")
        @Positive(message = "dimension must be greater than 0")
        Integer dimension
) {
}
