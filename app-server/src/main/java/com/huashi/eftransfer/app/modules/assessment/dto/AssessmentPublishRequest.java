package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AssessmentPublishRequest(
        Long teachingClassId,
        LocalDateTime startsAt,
        LocalDateTime dueAt,
        @Size(max = 1000, message = "instructionsText must be at most 1000 characters")
        String instructionsText,
        @Size(max = 32, message = "resultReleasePolicy must be at most 32 characters")
        String resultReleasePolicy,
        @Size(max = 32, message = "deliveryMode must be at most 32 characters")
        String deliveryMode,
        @Min(1) @Max(5000) Integer participantCodeCount
) {

    public AssessmentPublishRequest(
            Long teachingClassId, LocalDateTime startsAt, LocalDateTime dueAt,
            String instructionsText, String resultReleasePolicy
    ) {
        this(teachingClassId, startsAt, dueAt, instructionsText, resultReleasePolicy, "CLASS", null);
    }
}
