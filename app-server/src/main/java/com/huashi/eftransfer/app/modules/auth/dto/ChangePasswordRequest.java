package com.huashi.eftransfer.app.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "currentPassword must not be blank")
        @Size(max = 128, message = "currentPassword must be at most 128 characters")
        String currentPassword,
        @NotBlank(message = "newPassword must not be blank")
        @Size(min = 8, max = 128, message = "newPassword must be between 8 and 128 characters")
        String newPassword
) {
}
