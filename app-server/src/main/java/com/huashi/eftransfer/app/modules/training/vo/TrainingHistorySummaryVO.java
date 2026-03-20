package com.huashi.eftransfer.app.modules.training.vo;

import java.time.LocalDateTime;

public record TrainingHistorySummaryVO(
        Long sessionId,
        Long planId,
        Long ownerUserId,
        String status,
        String mode,
        Integer totalItems,
        Integer answeredItems,
        Integer currentItemOrder,
        LocalDateTime startedAt,
        LocalDateTime lastSavedAt,
        LocalDateTime completedAt
) {
}
