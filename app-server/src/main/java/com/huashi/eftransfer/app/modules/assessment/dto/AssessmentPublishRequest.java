package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AssessmentPublishRequest(
        @NotNull(message = "teachingClassId must not be null")
        Long teachingClassId,
        LocalDateTime startsAt,
        LocalDateTime dueAt,
        @Size(max = 1000, message = "instructionsText must be at most 1000 characters")
        String instructionsText
) {
}
