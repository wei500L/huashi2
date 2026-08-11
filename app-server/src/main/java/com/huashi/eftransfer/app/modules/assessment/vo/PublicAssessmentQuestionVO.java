package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record PublicAssessmentQuestionVO(
        Long questionId,
        Integer questionOrder,
        String questionType,
        String itemCode,
        String sectionCode,
        String sectionTitle,
        String sectionInstruction,
        String sharedMaterial,
        String stemText,
        String promptText,
        PublicAssessmentQuestionPresentationVO presentation,
        List<AssessmentOptionVO> options,
        boolean required,
        boolean justificationRequired,
        List<String> responses,
        String justificationText
) {
}
