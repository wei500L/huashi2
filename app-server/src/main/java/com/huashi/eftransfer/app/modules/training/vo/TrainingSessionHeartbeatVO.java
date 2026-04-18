package com.huashi.eftransfer.app.modules.training.vo;

import com.huashi.eftransfer.shared.enums.TrainingSessionStatus;

import java.time.LocalDateTime;

public record TrainingSessionHeartbeatVO(
        Long sessionId,
        TrainingSessionStatus status,
        Integer answeredItems,
        Integer currentItemOrder,
        LocalDateTime lastSavedAt
) {
}
