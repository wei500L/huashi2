package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record AssessmentPublishRosterItemVO(
        Long studentUserId,
        String studentName,
        String attemptStatus,
        Long attemptId,
        Integer answeredCount,
        Integer questionCount,
        Integer objectiveScore,
        Integer totalScore,
        LocalDateTime startedAt,
        LocalDateTime expiresAt,
        LocalDateTime submittedAt,
        LocalDateTime lastSavedAt
) {
}
