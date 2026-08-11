package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record PublicAssessmentProfileFieldVO(
        String itemCode,
        String questionType,
        String label,
        String promptText,
        List<AssessmentOptionVO> options,
        boolean required,
        PublicAssessmentDisplayConditionVO displayCondition
) {
    public PublicAssessmentProfileFieldVO {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
