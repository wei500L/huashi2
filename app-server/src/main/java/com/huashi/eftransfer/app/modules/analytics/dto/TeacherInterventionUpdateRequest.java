package com.huashi.eftransfer.app.modules.analytics.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record TeacherInterventionUpdateRequest(
        @Pattern(
                regexp = "^(LOW|NORMAL|URGENT)$",
                message = "priority must be one of LOW, NORMAL, or URGENT"
        )
        String priority,
        @Pattern(
                regexp = "^(PENDING|IN_PROGRESS|COMPLETED)$",
                message = "status must be one of PENDING, IN_PROGRESS, or COMPLETED"
        )
        String status,
        LocalDateTime plannedAt,
        @Size(max = 2000, message = "teacherNote must be less than or equal to 2000 characters")
        String teacherNote
) {
}
