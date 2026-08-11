package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;
import java.util.List;

public record PublicAssessmentMetadataVO(
        String releaseCode,
        String title,
        String description,
        String instructionsText,
        Integer durationMinutes,
        Integer questionCount,
        String status,
        LocalDateTime startsAt,
        LocalDateTime dueAt,
        boolean qrEntryEnabled,
        List<PublicAssessmentProfileFieldVO> profileFields
) {
    public PublicAssessmentMetadataVO {
        profileFields = profileFields == null ? List.of() : List.copyOf(profileFields);
    }
}
