package com.huashi.eftransfer.app.modules.diagnosis.vo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record DiagnosisTemplateDraftValidationResponseVO(
        boolean valid,
        Map<String, String> fieldErrors,
        List<DiagnosisTemplateDraftItemValidationVO> itemErrors,
        Set<String> blockingSteps
) {
}
