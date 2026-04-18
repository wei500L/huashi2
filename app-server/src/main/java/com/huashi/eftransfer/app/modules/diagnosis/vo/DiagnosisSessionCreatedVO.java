package com.huashi.eftransfer.app.modules.diagnosis.vo;

import com.huashi.eftransfer.shared.enums.DiagnosisSessionStatus;

import java.time.LocalDateTime;

public record DiagnosisSessionCreatedVO(
        Long sessionId,
        Long templateId,
        String templateName,
        DiagnosisSessionStatus status,
        Integer totalItems,
        Integer answeredItems,
        Integer currentItemOrder,
        LocalDateTime startedAt
) {
}
