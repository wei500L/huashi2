package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SaveAssessmentResponsesRequest(
        @Valid
        @NotEmpty(message = "responses must not be empty")
        List<AssessmentAttemptResponseRequest> responses
) {
}
