package com.huashi.eftransfer.app.modules.training.vo;

import java.util.List;

public record TrainingSessionSummaryVO(
        Long sessionId,
        String mode,
        double accuracy,
        long averageReactionTime,
        String improvementHint,
        String nextRecommendedMode,
        List<TrainingRiskWordVO> riskWordsToReview,
        List<TrainingItemResultDetailVO> items
) {
}
