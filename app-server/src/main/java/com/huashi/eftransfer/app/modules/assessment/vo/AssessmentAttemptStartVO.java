package com.huashi.eftransfer.app.modules.assessment.vo;

import com.huashi.eftransfer.shared.enums.AssessmentAttemptStatus;

public record AssessmentAttemptStartVO(
        Long attemptId,
        Long publishId,
        AssessmentAttemptStatus status,
        boolean resumed
) {
}
