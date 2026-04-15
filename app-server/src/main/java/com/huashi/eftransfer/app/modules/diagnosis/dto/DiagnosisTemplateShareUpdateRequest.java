package com.huashi.eftransfer.app.modules.diagnosis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiagnosisTemplateShareUpdateRequest(
        @NotBlank(message = "shareScope must not be blank")
        @Size(max = 16, message = "shareScope must be less than or equal to 16 characters")
        String shareScope
) {
}
