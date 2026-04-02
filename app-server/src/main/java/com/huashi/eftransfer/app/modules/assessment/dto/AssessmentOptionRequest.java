package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.NotBlank;

public record AssessmentOptionRequest(
        @NotBlank(message = "key must not be blank")
        String key,
        @NotBlank(message = "label must not be blank")
        String label
) {
}
