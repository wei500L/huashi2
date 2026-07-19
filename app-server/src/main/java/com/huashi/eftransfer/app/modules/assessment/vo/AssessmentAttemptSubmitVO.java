package com.huashi.eftransfer.app.modules.assessment.vo;

import com.huashi.eftransfer.shared.enums.AssessmentAttemptStatus;

import java.time.LocalDateTime;

public record AssessmentAttemptSubmitVO(
        Long attemptId,
        AssessmentAttemptStatus status,
        LocalDateTime submittedAt,
        Long version,
        String submitReason
) {
}
