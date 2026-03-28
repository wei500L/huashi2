package com.huashi.eftransfer.app.modules.diagnosis.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record DiagnosisTemplateDraftSaveRequest(
        @NotNull(message = "version must not be null")
        Long version,
        @NotNull(message = "schema must not be null")
        @Valid DiagnosisTemplateDraftSchemaRequest schema
) {
}
