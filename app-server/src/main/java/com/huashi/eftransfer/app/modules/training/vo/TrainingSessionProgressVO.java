package com.huashi.eftransfer.app.modules.training.vo;

public record TrainingSessionProgressVO(
        Long sessionId,
        String status,
        Integer totalItems,
        Integer answeredItems,
        Integer currentItemOrder,
        Boolean completed
) {
}
