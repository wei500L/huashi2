package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicAssessmentQrEntryRequest(
        @NotBlank @Size(min = 16, max = 128) String browserFingerprint
) {
}
