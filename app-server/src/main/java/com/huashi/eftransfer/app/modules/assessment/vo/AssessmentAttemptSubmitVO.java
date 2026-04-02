package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record AssessmentAttemptSubmitVO(
        Long attemptId,
        String status,
        LocalDateTime submittedAt
) {
}
