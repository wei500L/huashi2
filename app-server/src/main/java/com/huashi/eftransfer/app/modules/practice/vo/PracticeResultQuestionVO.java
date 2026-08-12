package com.huashi.eftransfer.app.modules.practice.vo;

import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentOptionVO;

import java.util.List;
import java.util.Map;

/**
 * A fully reviewed question on the practice result page: correct answer,
 * student response, correctness, and the bank explanation are all exposed.
 */
public record PracticeResultQuestionVO(
        Integer questionOrder,
        String questionCode,
        String questionType,
        String sectionCode,
        String constructCode,
        String transferCategory,
        String targetWord,
        String stemText,
        String promptText,
        List<AssessmentOptionVO> options,
        List<String> correctAnswer,
        List<String> response,
        Boolean correct,
        String explanation,
        Map<String, String> optionExplanations,
        Boolean spellingHintShown,
        Integer spellingWrongAttemptCount,
        String spellingErrorPattern
) {
}
