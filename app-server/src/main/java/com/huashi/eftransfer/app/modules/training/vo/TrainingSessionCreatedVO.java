package com.huashi.eftransfer.app.modules.training.vo;

public record TrainingSessionCreatedVO(
        Long sessionId,
        Long planId,
        String status,
        String mode,
        Integer totalItems,
        Integer answeredItems,
        Integer currentItemOrder
) {
}
