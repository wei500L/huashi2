package com.huashi.eftransfer.app.modules.practice.vo;

import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentOptionVO;

import java.util.List;

/**
 * A practice question as shown while answering: correct answers and
 * explanations are withheld until the session is completed.
 */
public record PracticeQuestionVO(
        Integer questionOrder,
        String questionCode,
        String questionType,
        String stemText,
        String promptText,
        List<AssessmentOptionVO> options,
        String sectionCode,
        String constructCode,
        String transferCategory,
        String targetWord,
        List<String> response,
        Boolean spellingHintShown,
        String spellingHintFirstLetter,
        Integer spellingWrongAttemptCount,
        Boolean answered
) {
}
