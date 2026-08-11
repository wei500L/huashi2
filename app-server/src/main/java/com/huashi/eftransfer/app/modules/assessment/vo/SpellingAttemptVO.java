package com.huashi.eftransfer.app.modules.assessment.vo;

/**
 * Result of grading one spelling candidate.
 *
 * - hintShown becomes true after the first wrong attempt and stays true.
 * - hintFirstLetter contains only the first character of the target word and
 *   is returned only when the hint has been shown; the complete answer is
 *   never exposed here.
 * - wrongAttemptCount counts wrong candidate submissions for this question.
 */
public record SpellingAttemptVO(
        boolean correct,
        boolean hintShown,
        String hintFirstLetter,
        int wrongAttemptCount
) {
}
