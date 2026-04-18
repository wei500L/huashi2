package com.huashi.eftransfer.app.modules.training.vo;

import com.huashi.eftransfer.shared.enums.TrainingMode;
import com.huashi.eftransfer.shared.enums.TrainingSessionStatus;

import java.time.LocalDateTime;

public record TrainingHistorySummaryVO(
        Long sessionId,
        Long planId,
        Long ownerUserId,
        TrainingSessionStatus status,
        TrainingMode mode,
        Integer totalItems,
        Integer answeredItems,
        Integer currentItemOrder,
        LocalDateTime startedAt,
        LocalDateTime lastSavedAt,
        LocalDateTime completedAt
) {
}
