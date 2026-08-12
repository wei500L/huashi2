package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ResearchFileInitiateRequest(
        @NotNull Integer questionOrder,
        @NotBlank String fileName,
        String contentType,
        @NotNull @Positive Long sizeBytes
) {
}
