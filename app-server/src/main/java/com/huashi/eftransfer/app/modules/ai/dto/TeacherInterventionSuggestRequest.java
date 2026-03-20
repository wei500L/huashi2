package com.huashi.eftransfer.app.modules.ai.dto;

import jakarta.validation.constraints.NotNull;

public record TeacherInterventionSuggestRequest(
        @NotNull(message = "classId must not be null")
        Long classId,
        @NotNull(message = "studentUserId must not be null")
        Long studentUserId,
        Long diagnosisSummaryId
) {
}
