package com.huashi.eftransfer.app.modules.diagnosis.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SaveDiagnosisProgressRequest(
        @NotNull(message = "progressSnapshot must not be null")
        Map<String, Object> progressSnapshot
) {
}
