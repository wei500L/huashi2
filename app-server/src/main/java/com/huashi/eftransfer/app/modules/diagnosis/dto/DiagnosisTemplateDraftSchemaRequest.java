package com.huashi.eftransfer.app.modules.diagnosis.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DiagnosisTemplateDraftSchemaRequest(
        @Valid DiagnosisTemplateDraftBasicRequest basic,
        @NotNull(message = "items must not be null")
        List<@Valid DiagnosisTemplateDraftItemRequest> items
) {
}
