package com.huashi.eftransfer.shared.ai.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AiOpsChatConfig(
        @NotBlank(message = "baseUrl is required")
        String baseUrl,
        String apiKey,
        @NotBlank(message = "model is required")
        String model,
        @NotBlank(message = "timeout is required")
        String timeout,
        @NotNull(message = "temperature is required")
        @DecimalMin(value = "0.0", message = "temperature must be between 0 and 2")
        @DecimalMax(value = "2.0", message = "temperature must be between 0 and 2")
        Double temperature,
        @NotNull(message = "maxTokens is required")
        @Positive(message = "maxTokens must be greater than 0")
        Integer maxTokens
) {
}
