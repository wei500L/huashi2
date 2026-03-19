package com.huashi.eftransfer.app.modules.analytics.vo;

import java.time.LocalDateTime;

public record StudentProfileSummaryVO(
        Long studentUserId,
        String studentName,
        String gradeName,
        String primaryRiskLevel,
        double recentAccuracy,
        double recentNegativeTransferRisk,
        long recentAvgReactionTimeMs,
        int pendingReviewCount,
        String recommendedTrainingMode,
        LocalDateTime lastActiveAt
) {
}
