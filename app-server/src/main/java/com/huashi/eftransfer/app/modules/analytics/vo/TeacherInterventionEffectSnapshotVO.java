package com.huashi.eftransfer.app.modules.analytics.vo;

import java.time.LocalDateTime;

public record TeacherInterventionEffectSnapshotVO(
        Long snapshotId,
        LocalDateTime snapshotAt,
        String primaryRiskLevel,
        String recommendedTrainingMode,
        Integer pendingReviewCount,
        Integer highRiskPairCount,
        Double recentAccuracy,
        Double recentNegativeTransferRisk,
        Long recentAvgReactionTimeMs
) {
}
