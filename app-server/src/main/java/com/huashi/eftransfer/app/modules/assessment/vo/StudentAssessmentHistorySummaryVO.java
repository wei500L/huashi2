package com.huashi.eftransfer.app.modules.assessment.vo;

import com.huashi.eftransfer.shared.enums.AssessmentAttemptStatus;

import java.time.LocalDateTime;

public record StudentAssessmentHistorySummaryVO(
        Long attemptId,
        Long publishId,
        Long paperId,
        String title,
        String description,
        String className,
        AssessmentAttemptStatus status,
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
