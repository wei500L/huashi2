package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SubmitAssessmentAttemptRequest(
        @Valid
        List<AssessmentAttemptResponseRequest> responses,
        @Min(value = 1, message = "baseVersion must be greater than 0")
        Long baseVersion,
        String reason
) {
    public SubmitAssessmentAttemptRequest {
        responses = responses == null ? List.of() : List.copyOf(responses);
    }
}
