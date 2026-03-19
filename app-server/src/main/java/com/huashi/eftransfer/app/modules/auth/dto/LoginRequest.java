package com.huashi.eftransfer.app.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "usernameOrEmail must not be blank")
        String usernameOrEmail,
        @NotBlank(message = "password must not be blank")
        String password
) {
}
