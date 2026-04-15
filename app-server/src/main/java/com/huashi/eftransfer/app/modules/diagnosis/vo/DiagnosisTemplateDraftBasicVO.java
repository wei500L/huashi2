package com.huashi.eftransfer.app.modules.diagnosis.vo;

public record DiagnosisTemplateDraftBasicVO(
        String templateName,
        String description,
        String publishTarget,
        Integer estimatedDurationMinutes,
        Long targetClassId,
        String shareScope,
        String scoringVersion
) {
}
