package com.huashi.eftransfer.app.modules.diagnosis.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record DiagnosisTemplateScoringProfileRequest(
        @Size(max = 32, message = "formulaKey must be less than or equal to 32 characters")
        String formulaKey,
        @DecimalMin(value = "0.0", inclusive = false, message = "pairWeight must be greater than 0")
        @DecimalMax(value = "5.0", inclusive = true, message = "pairWeight must be less than or equal to 5")
        Double pairWeight,
        @DecimalMin(value = "0.0", inclusive = true, message = "riskAmplifier must be greater than or equal to 0")
        @DecimalMax(value = "5.0", inclusive = true, message = "riskAmplifier must be less than or equal to 5")
        Double riskAmplifier,
        @Min(value = 200, message = "maxReactionTimeMs must be greater than or equal to 200")
        @Max(value = 10000, message = "maxReactionTimeMs must be less than or equal to 10000")
        Integer maxReactionTimeMs
) {
}
