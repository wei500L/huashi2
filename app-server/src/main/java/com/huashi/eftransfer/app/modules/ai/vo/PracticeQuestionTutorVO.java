package com.huashi.eftransfer.app.modules.ai.vo;

import java.util.List;

/**
 * Personalized per-question tutoring of a practice item: an AI explanation
 * grounded in the question snapshot and the retrieved lexical knowledge, with
 * a rule fallback that returns the bank explanation text.
 */
public record PracticeQuestionTutorVO(
        Long practiceSessionId,
        Integer questionOrder,
        String generationSource,
        String explanation,
        String commonMistake,
        String memoryTip,
        List<String> relatedWords,
        String fallbackReason,
        String fallbackDetail
) {
}
