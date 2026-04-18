package com.huashi.eftransfer.app.modules.training.vo;

import com.huashi.eftransfer.shared.enums.TrainingMode;

import java.util.List;

public record TrainingSessionSummaryVO(
        Long sessionId,
        TrainingMode mode,
        double accuracy,
        long averageReactionTime,
        String improvementHint,
        TrainingMode nextRecommendedMode,
        List<TrainingRiskWordVO> riskWordsToReview,
        List<TrainingItemResultDetailVO> items
) {
}
