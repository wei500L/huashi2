package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record AssessmentAttemptResultQuestionVO(
        Long answerId,
        Long questionId,
        Integer questionOrder,
        String questionType,
        String stemText,
        String promptText,
        List<AssessmentOptionVO> options,
        Integer score,
        List<String> responses,
        List<String> correctAnswers,
        Boolean correct,
        Integer scoreAwarded,
        String explanationText
) {
}
