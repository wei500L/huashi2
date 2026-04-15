package com.huashi.eftransfer.app.modules.analytics.vo;

public record TeacherInterventionEffectDiffVO(
        Double recentAccuracyDelta,
        Double recentNegativeTransferRiskDelta,
        Long recentAvgReactionTimeMsDelta,
        Integer pendingReviewCountDelta,
        Integer highRiskPairCountDelta
) {
}
