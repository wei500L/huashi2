package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record PublicAssessmentQuestionVO(
        Long questionId,
        Integer questionOrder,
        String questionType,
        String sectionCode,
        String sectionTitle,
        String sharedMaterial,
        String stemText,
        String promptText,
        List<AssessmentOptionVO> options,
        boolean required,
        boolean justificationRequired,
        List<String> responses,
        String justificationText
) {
}
