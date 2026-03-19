package com.huashi.eftransfer.app.modules.diagnosis.vo;

import java.time.LocalDateTime;

public record DiagnosisSessionCreatedVO(
        Long sessionId,
        Long templateId,
        String templateName,
        String status,
        Integer totalItems,
        Integer answeredItems,
        Integer currentItemOrder,
        LocalDateTime startedAt
) {
}
