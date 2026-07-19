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
        String resultReleasePolicy,
        LocalDateTime publishedAt,
        Integer assignedCount,
        Integer attemptCount,
        Integer submittedCount,
        Integer pendingCount
) {
}
