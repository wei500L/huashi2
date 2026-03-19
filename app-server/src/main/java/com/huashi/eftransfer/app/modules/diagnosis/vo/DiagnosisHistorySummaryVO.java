package com.huashi.eftransfer.app.modules.diagnosis.vo;

import java.time.LocalDateTime;

public record DiagnosisHistorySummaryVO(
        Long sessionId,
        Long templateId,
        String templateName,
        Long ownerUserId,
        String status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        Double positiveTransferScore,
        Double negativeTransferRisk,
        Double overallAccuracy
) {
}
