package com.huashi.eftransfer.app.modules.diagnosis.vo;

import java.time.LocalDateTime;

public record DiagnosisTemplateDraftDetailVO(
        Long draftId,
        Long sourceTemplateId,
        Long publishedTemplateId,
        String syncState,
        Long version,
        DiagnosisTemplateDraftSchemaVO schema,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
