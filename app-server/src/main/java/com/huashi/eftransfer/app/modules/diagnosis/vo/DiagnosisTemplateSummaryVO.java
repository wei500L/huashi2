package com.huashi.eftransfer.app.modules.diagnosis.vo;

import java.time.LocalDateTime;

public record DiagnosisTemplateSummaryVO(
        Long id,
        String templateName,
        String description,
        String status,
        Long targetClassId,
        String targetClassName,
        Integer itemCount,
        Integer estimatedDurationMinutes,
        String scoringVersion,
        String shareScope,
        Long ownerUserId,
        String ownerDisplayName,
        LocalDateTime updatedAt
) {
}
