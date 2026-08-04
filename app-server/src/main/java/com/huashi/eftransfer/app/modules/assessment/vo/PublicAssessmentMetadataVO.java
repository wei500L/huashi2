package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record PublicAssessmentMetadataVO(
        String releaseCode,
        String title,
        String description,
        String instructionsText,
        Integer durationMinutes,
        Integer questionCount,
        String status,
        LocalDateTime startsAt,
        LocalDateTime dueAt
) {
}
