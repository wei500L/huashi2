package com.huashi.eftransfer.app.modules.diagnosis.vo;

import java.util.Map;

public record DiagnosisTemplateDraftItemValidationVO(
        String draftItemId,
        Integer itemIndex,
        Map<String, String> fieldErrors
) {
}
