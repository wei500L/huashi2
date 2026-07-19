package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitAssessmentAttemptRequest(
        @Valid
        @NotNull(message = "responses must not be null")
        List<AssessmentAttemptResponseRequest> responses,
        @NotNull(message = "baseVersion must not be null")
        @Min(value = 1, message = "baseVersion must be greater than 0")
        Long baseVersion,
        String reason
) {
}
