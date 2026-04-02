package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssessmentAttemptResponseRequest(
        @NotNull(message = "questionOrder must not be null")
        Integer questionOrder,
        List<String> responses
) {
}
