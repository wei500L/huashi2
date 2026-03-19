package com.huashi.eftransfer.app.modules.diagnosis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiagnosisTemplateOptionRequest(
        @NotBlank(message = "key must not be blank")
        @Size(max = 64, message = "key must be less than or equal to 64 characters")
        String key,
        @NotBlank(message = "label must not be blank")
        @Size(max = 255, message = "label must be less than or equal to 255 characters")
        String label,
        Boolean semanticMatch,
        Boolean ignoreContextTrap
) {
}
