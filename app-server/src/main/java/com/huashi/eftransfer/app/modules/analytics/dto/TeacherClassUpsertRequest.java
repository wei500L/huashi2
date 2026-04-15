package com.huashi.eftransfer.app.modules.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeacherClassUpsertRequest(
        @NotBlank(message = "classCode must not be blank")
        @Size(max = 64, message = "classCode must be at most 64 characters")
        String classCode,
        @NotBlank(message = "className must not be blank")
        @Size(max = 128, message = "className must be at most 128 characters")
        String className,
        @NotBlank(message = "gradeName must not be blank")
        @Size(max = 64, message = "gradeName must be at most 64 characters")
        String gradeName
) {
}
