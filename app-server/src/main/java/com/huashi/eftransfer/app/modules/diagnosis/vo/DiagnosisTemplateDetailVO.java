package com.huashi.eftransfer.app.modules.diagnosis.vo;

import java.time.LocalDateTime;
import java.util.List;

public record DiagnosisTemplateDetailVO(
        Long id,
        String templateName,
        String description,
        String status,
        Integer estimatedDurationMinutes,
        String scoringVersion,
        Integer itemCount,
        Long ownerUserId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<DiagnosisTemplateItemVO> items
) {
}
