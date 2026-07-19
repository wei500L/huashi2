package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record StudentAssessmentSummaryVO(
        Long publishId,
        Long paperId,
        String title,
        String description,
        Long teachingClassId,
        String className,
        String instructionsText,
        Integer durationMinutes,
        Integer questionCount,
        Integer totalScore,
        LocalDateTime startsAt,
        LocalDateTime dueAt,
        LocalDateTime publishedAt,
        String attemptStatus,
        Long attemptId,
        Integer answeredCount,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        LocalDateTime submittedAt,
        String releaseStatus,
        LocalDateTime resultAvailableAt
) {
}
