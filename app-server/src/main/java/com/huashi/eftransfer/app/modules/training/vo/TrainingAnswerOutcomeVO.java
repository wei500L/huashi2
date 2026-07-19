package com.huashi.eftransfer.app.modules.training.vo;

public record TrainingAnswerOutcomeVO(
        boolean correct,
        String selectedAnswerKey,
        String correctAnswerKey,
        String detectedErrorType,
        String explanation,
        String adaptationAction
) {
}
