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
        @NotBlank(message = "timeout is required")
        String timeout,
        @NotNull(message = "dimension is required")
        @Positive(message = "dimension must be greater than 0")
        Integer dimension
) {
}
