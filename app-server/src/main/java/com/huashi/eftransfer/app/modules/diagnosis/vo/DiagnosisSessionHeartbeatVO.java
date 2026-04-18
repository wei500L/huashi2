package com.huashi.eftransfer.app.modules.diagnosis.vo;

import com.huashi.eftransfer.shared.enums.DiagnosisSessionStatus;

import java.time.LocalDateTime;

public record DiagnosisSessionHeartbeatVO(
        Long sessionId,
        DiagnosisSessionStatus status,
        Integer answeredItems,
        Integer currentItemOrder,
        LocalDateTime lastSavedAt
) {
}
