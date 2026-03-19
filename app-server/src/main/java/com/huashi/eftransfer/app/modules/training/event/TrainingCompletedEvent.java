package com.huashi.eftransfer.app.modules.training.event;

import java.time.LocalDateTime;

public record TrainingCompletedEvent(
        Long sessionId,
        Long planId,
        Long ownerUserId,
        String mode,
        LocalDateTime completedAt,
        double accuracy,
        long averageReactionTime,
        String nextRecommendedMode,
        int pendingReviewCount,
        String traceId,
        int eventVersion
) {
}
