package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record PublicAssessmentMetadataVO(
        String releaseCode,
        String title,
        String description,
        String instructionsText,
        Integer durationMinutes,
        Integer questionCount,
        Integer formalQuestionCount,
        Integer profileFieldCount,
        String status,
        LocalDateTime startsAt,
        LocalDateTime dueAt,
        boolean qrEntryEnabled,
        int maxAttempts
) {
}
