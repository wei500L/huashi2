package com.huashi.eftransfer.app.modules.diagnosis.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DiagnosisTemplateUpsertRequest(
        @NotBlank(message = "templateName must not be blank")
        @Size(max = 128, message = "templateName must be less than or equal to 128 characters")
        String templateName,
        @Size(max = 500, message = "description must be less than or equal to 500 characters")
        String description,
        @NotBlank(message = "status must not be blank")
        String status,
        @NotNull(message = "estimatedDurationMinutes must not be null")
        @Min(value = 1, message = "estimatedDurationMinutes must be greater than 0")
        @Max(value = 180, message = "estimatedDurationMinutes must be less than or equal to 180")
        Integer estimatedDurationMinutes,
        Long targetClassId,
        @Size(max = 16, message = "shareScope must be less than or equal to 16 characters")
        String shareScope,
        @Size(max = 32, message = "scoringVersion must be less than or equal to 32 characters")
        String scoringVersion,
        @NotNull(message = "items must not be null")
        List<@Valid DiagnosisTemplateItemRequest> items
) {
}
