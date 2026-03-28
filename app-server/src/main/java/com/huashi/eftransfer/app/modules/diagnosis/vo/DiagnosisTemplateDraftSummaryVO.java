package com.huashi.eftransfer.app.modules.diagnosis.vo;

import java.time.LocalDateTime;

public record DiagnosisTemplateDraftSummaryVO(
        Long draftId,
        Long sourceTemplateId,
        Long publishedTemplateId,
        String templateName,
        String description,
        String syncState,
        Long version,
        LocalDateTime updatedAt
) {
}
