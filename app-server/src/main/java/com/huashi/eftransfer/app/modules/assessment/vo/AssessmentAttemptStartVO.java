package com.huashi.eftransfer.app.modules.assessment.vo;

public record AssessmentAttemptStartVO(
        Long attemptId,
        Long publishId,
        String status,
        boolean resumed
) {
}
