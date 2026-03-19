package com.huashi.eftransfer.app.modules.diagnosis.vo;

import java.time.LocalDateTime;

public record DiagnosisTemplateSummaryVO(
        Long id,
        String templateName,
        String description,
        String status,
        Integer itemCount,
        Integer estimatedDurationMinutes,
        String scoringVersion,
        Long ownerUserId,
        LocalDateTime updatedAt
) {
}
