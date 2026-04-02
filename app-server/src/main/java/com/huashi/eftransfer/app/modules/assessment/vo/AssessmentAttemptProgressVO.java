package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record AssessmentAttemptProgressVO(
        Long attemptId,
        String status,
        Integer answeredCount,
        LocalDateTime lastSavedAt
) {
}
