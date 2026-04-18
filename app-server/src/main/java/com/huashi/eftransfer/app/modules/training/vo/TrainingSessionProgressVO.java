package com.huashi.eftransfer.app.modules.training.vo;

import com.huashi.eftransfer.shared.enums.TrainingSessionStatus;

public record TrainingSessionProgressVO(
        Long sessionId,
        TrainingSessionStatus status,
        Integer totalItems,
        Integer answeredItems,
        Integer currentItemOrder,
        Boolean completed,
        Boolean readyToComplete
) {
}
