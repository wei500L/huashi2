package com.huashi.eftransfer.app.modules.diagnosis.dto;

import jakarta.validation.constraints.Size;

public record DiagnosisTemplateDraftBasicRequest(
        @Size(max = 128, message = "templateName must be less than or equal to 128 characters")
        String templateName,
        @Size(max = 500, message = "description must be less than or equal to 500 characters")
        String description,
        @Size(max = 32, message = "publishTarget must be less than or equal to 32 characters")
        String publishTarget,
        Integer estimatedDurationMinutes,
        @Size(max = 32, message = "scoringVersion must be less than or equal to 32 characters")
        String scoringVersion
) {
}
