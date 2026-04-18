package com.huashi.eftransfer.app.modules.diagnosis.dto;

import jakarta.validation.constraints.NotNull;

public record CreateDiagnosisSessionRequest(
        @NotNull(message = "templateId must not be null")
        Long templateId,
        String launchSource,
        Long sourceSummaryId
) {
}
