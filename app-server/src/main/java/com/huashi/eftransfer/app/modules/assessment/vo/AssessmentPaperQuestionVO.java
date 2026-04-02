package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record AssessmentPaperQuestionVO(
        Long questionId,
        String questionType,
        Integer sortOrder,
        String stemText,
        String promptText,
        List<AssessmentOptionVO> options,
        List<String> correctAnswers,
        String explanationText,
        Integer score
) {
}
