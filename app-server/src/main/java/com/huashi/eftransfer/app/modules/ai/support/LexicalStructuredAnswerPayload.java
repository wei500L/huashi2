package com.huashi.eftransfer.app.modules.ai.support;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record LexicalStructuredAnswerPayload(
        @NotBlank(message = "answer must not be blank")
        String answer,
        @NotBlank(message = "explanation must not be blank")
        String explanation,
        @NotEmpty(message = "recommendedActions must not be empty")
        List<@NotBlank(message = "recommendedActions item must not be blank") String> recommendedActions,
        @DecimalMin(value = "0.0", message = "confidence must be >= 0")
        @DecimalMax(value = "1.0", message = "confidence must be <= 1")
        double confidence,
        @NotEmpty(message = "citationIds must not be empty")
        List<@NotBlank(message = "citationIds item must not be blank") String> citationIds
) {
}
