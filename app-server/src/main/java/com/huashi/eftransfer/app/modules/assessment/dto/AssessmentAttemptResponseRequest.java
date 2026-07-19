package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AssessmentAttemptResponseRequest(
        @NotNull(message = "questionOrder must not be null")
        Integer questionOrder,
        List<@Size(max = 1000, message = "response must be at most 1000 characters") String> responses
) {
}
