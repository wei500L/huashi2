package com.huashi.eftransfer.app.modules.diagnosis.vo;

import com.huashi.eftransfer.shared.enums.DiagnosisSessionStatus;

public record DiagnosisSessionProgressVO(
        Long sessionId,
        DiagnosisSessionStatus status,
        Integer totalItems,
        Integer answeredItems,
        Integer currentItemOrder,
        Boolean completed,
        Boolean readyToComplete
) {
}
