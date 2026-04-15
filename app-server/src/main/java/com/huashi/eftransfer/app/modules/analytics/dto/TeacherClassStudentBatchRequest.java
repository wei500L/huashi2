package com.huashi.eftransfer.app.modules.analytics.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TeacherClassStudentBatchRequest(
        @NotEmpty(message = "studentUserIds must not be empty")
        List<@NotNull(message = "studentUserIds must not contain null values") Long> studentUserIds
) {
}
