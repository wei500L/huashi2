package com.huashi.eftransfer.app.modules.assessment.vo;

import com.huashi.eftransfer.shared.enums.AssessmentAttemptStatus;

import java.time.LocalDateTime;

public record AssessmentAttemptHeartbeatVO(
        Long attemptId,
        AssessmentAttemptStatus status,
        Integer answeredCount,
        LocalDateTime expiresAt,
        LocalDateTime submittedAt,
        LocalDateTime lastSavedAt,
        LocalDateTime serverTime
) {
}
