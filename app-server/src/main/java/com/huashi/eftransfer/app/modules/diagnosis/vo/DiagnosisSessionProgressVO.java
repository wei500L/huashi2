package com.huashi.eftransfer.app.modules.diagnosis.vo;

public record DiagnosisSessionProgressVO(
        Long sessionId,
        String status,
        Integer totalItems,
        Integer answeredItems,
        Integer currentItemOrder,
        Boolean completed,
        Boolean readyToComplete
) {
}
