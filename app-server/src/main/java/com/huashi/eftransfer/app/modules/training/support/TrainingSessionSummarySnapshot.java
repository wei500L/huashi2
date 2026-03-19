package com.huashi.eftransfer.app.modules.training.support;

import java.util.List;

public record TrainingSessionSummarySnapshot(
        double accuracy,
        long averageReactionTime,
        String improvementHint,
        String nextRecommendedMode,
        List<TrainingRiskWordSnapshot> riskWordsToReview
) {
}
