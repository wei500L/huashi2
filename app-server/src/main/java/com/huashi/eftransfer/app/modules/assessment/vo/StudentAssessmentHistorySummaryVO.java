package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record StudentAssessmentHistorySummaryVO(
        Long attemptId,
        Long publishId,
        Long paperId,
        String title,
        String description,
        String className,
        String status,
        Integer questionCount,
        Integer answeredCount,
        Integer objectiveScore,
        Integer totalScore,
        LocalDateTime startedAt,
        LocalDateTime lastSavedAt,
        LocalDateTime expiresAt,
        LocalDateTime submittedAt
) {
}
