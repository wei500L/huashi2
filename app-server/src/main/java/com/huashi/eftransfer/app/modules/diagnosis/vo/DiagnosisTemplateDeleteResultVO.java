package com.huashi.eftransfer.app.modules.diagnosis.vo;

public record DiagnosisTemplateDeleteResultVO(
        Long templateId,
        String outcome,
        String status
) {
}
