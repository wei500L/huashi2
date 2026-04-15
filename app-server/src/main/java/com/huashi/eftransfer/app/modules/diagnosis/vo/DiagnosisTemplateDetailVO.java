package com.huashi.eftransfer.app.modules.diagnosis.vo;

import java.time.LocalDateTime;
import java.util.List;

public record DiagnosisTemplateDetailVO(
        Long id,
        String templateName,
        String description,
        String status,
        Long targetClassId,
        String targetClassName,
        Integer estimatedDurationMinutes,
        String scoringVersion,
        Integer itemCount,
        String shareScope,
        Long ownerUserId,
        String ownerDisplayName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<DiagnosisTemplateItemVO> items
) {
}
