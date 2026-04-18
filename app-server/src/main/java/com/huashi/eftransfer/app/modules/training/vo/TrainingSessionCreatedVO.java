package com.huashi.eftransfer.app.modules.training.vo;

import com.huashi.eftransfer.shared.enums.TrainingMode;
import com.huashi.eftransfer.shared.enums.TrainingSessionStatus;

public record TrainingSessionCreatedVO(
        Long sessionId,
        Long planId,
        TrainingSessionStatus status,
        TrainingMode mode,
        Integer totalItems,
        Integer answeredItems,
        Integer currentItemOrder
) {
}
