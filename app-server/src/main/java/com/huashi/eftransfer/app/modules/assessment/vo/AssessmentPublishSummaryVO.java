package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record AssessmentPublishSummaryVO(
        Long publishId,
        Long teachingClassId,
        String className,
        String status,
        Integer durationMinutes,
        Integer questionCount,
        Integer totalScore,
        String instructionsText,
        LocalDateTime startsAt,
        LocalDateTime dueAt,
        LocalDateTime publishedAt,
        Integer attemptCount,
        Integer submittedCount
) {
}
