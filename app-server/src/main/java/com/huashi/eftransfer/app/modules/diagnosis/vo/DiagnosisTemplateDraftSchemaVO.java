package com.huashi.eftransfer.app.modules.diagnosis.vo;

import java.util.List;

public record DiagnosisTemplateDraftSchemaVO(
        DiagnosisTemplateDraftBasicVO basic,
        List<DiagnosisTemplateDraftItemVO> items
) {
}
