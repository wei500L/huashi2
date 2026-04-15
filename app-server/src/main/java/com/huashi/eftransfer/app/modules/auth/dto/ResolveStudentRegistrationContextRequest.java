package com.huashi.eftransfer.app.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolveStudentRegistrationContextRequest(
        @NotBlank(message = "classCode must not be blank")
        @Size(max = 64, message = "classCode must be at most 64 characters")
        String classCode
) {
}
