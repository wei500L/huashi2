package com.huashi.eftransfer.app.modules.assessment.vo;

public record PublicAssessmentSessionVO(
        boolean verified,
        boolean resumed,
        String releaseCode,
        PublicAssessmentAttemptVO attempt
) {
}
