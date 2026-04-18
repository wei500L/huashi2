package com.huashi.eftransfer.app.modules.diagnosis.vo;

import com.huashi.eftransfer.shared.enums.DiagnosisSessionStatus;

import java.time.LocalDateTime;

public record DiagnosisHistorySummaryVO(
        Long sessionId,
        Long summaryId,
        Long templateId,
        String templateName,
        Long ownerUserId,
        DiagnosisSessionStatus status,
        LocalDateTime startedAt,
        LocalDateTime lastSavedAt,
        LocalDateTime completedAt,
        Double positiveTransferScore,
        Double negativeTransferRisk,
        Double overallAccuracy
) {
}
