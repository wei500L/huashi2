package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record PublicAssessmentProfileRequest(
        boolean consentAccepted,
        @NotNull Map<String, Object> values
) {
    public PublicAssessmentProfileRequest {
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
