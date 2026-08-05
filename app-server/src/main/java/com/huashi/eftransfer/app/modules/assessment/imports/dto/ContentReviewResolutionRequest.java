package com.huashi.eftransfer.app.modules.assessment.imports.dto;

import jakarta.validation.constraints.NotBlank;

public record ContentReviewResolutionRequest(
        @NotBlank String decision,
        @NotBlank String resolutionNote
) {
}
