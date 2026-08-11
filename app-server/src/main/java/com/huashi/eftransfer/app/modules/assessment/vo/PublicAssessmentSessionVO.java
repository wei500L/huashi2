package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record PublicAssessmentSessionVO(
        boolean verified,
        boolean resumed,
        String releaseCode,
        boolean profileRequired,
        List<PublicAssessmentProfileFieldVO> profileFields,
        PublicAssessmentAttemptVO attempt
) {
    public PublicAssessmentSessionVO {
        profileFields = profileFields == null ? List.of() : List.copyOf(profileFields);
    }
}
