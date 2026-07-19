package com.huashi.eftransfer.shared.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ChatRequest(
        @NotEmpty(message = "messages must not be empty")
        @Size(max = 64, message = "messages size must be less than or equal to 64")
        List<@Valid ChatMessage> messages,
        @Size(max = 128, message = "model is too long")
        String model,
        @DecimalMin(value = "0.0", message = "temperature must be >= 0")
        @DecimalMax(value = "2.0", message = "temperature must be <= 2")
        Double temperature,
        @Positive(message = "maxTokens must be greater than 0")
        @Max(value = 32768, message = "maxTokens must be less than or equal to 32768")
        Integer maxTokens,
        @Pattern(regexp = "none|low|medium|high|xhigh|max", message = "reasoningEffort is invalid")
        String reasoningEffort,
        Boolean proMode
) {

    public ChatRequest(
            List<ChatMessage> messages,
            String model,
            Double temperature,
            Integer maxTokens
    ) {
        this(messages, model, temperature, maxTokens, null, null);
    }
}
