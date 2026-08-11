package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PublicAssessmentTimingRequest(
        @NotNull(message = "questionOrder must not be null")
        @Min(value = 1, message = "questionOrder must be greater than 0")
        Integer questionOrder,
        @NotNull(message = "activeDurationMs must not be null")
        @Min(value = 0, message = "activeDurationMs must not be negative")
        @Max(value = 30000, message = "activeDurationMs must not exceed 30 seconds")
        Long activeDurationMs,
        @NotBlank(message = "eventId must not be blank")
        @Size(max = 64, message = "eventId must be at most 64 characters")
        String eventId
) {
}
