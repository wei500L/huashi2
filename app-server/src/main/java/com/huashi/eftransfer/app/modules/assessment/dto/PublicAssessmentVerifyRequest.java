package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record PublicAssessmentVerifyRequest(
        @NotBlank @Size(max = 14) String participationCode,
        Map<String, Object> basicInfo
) {
}
