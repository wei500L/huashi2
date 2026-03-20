package com.huashi.eftransfer.shared.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record ChatRequest(
        @NotEmpty(message = "messages must not be empty")
        List<@Valid ChatMessage> messages,
        String model,
        @DecimalMin(value = "0.0", message = "temperature must be >= 0")
        @DecimalMax(value = "2.0", message = "temperature must be <= 2")
        Double temperature,
        @Positive(message = "maxTokens must be greater than 0")
        Integer maxTokens
) {
}
