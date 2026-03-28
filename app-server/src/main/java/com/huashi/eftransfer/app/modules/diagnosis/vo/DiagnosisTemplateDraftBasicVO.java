package com.huashi.eftransfer.app.modules.diagnosis.vo;

public record DiagnosisTemplateDraftBasicVO(
        String templateName,
        String description,
        String publishTarget,
        Integer estimatedDurationMinutes,
        String scoringVersion
) {
}
