package com.huashi.eftransfer.app.modules.practice.vo;

public record PracticeSpellingCheckVO(
        boolean correct,
        boolean hintShown,
        String hintFirstLetter,
        int wrongAttemptCount
) {
}
