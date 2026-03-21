package com.huashi.eftransfer.app.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "usernameOrEmail must not be blank")
        @Size(max = 128, message = "usernameOrEmail must be at most 128 characters")
        String usernameOrEmail,
        @NotBlank(message = "password must not be blank")
        @Size(max = 128, message = "password must be at most 128 characters")
        String password
) {
}
