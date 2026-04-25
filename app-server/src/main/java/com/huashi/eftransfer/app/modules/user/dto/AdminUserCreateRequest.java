package com.huashi.eftransfer.app.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AdminUserCreateRequest(
        @NotBlank(message = "username must not be blank")
        @Size(max = 64, message = "username must be less than or equal to 64 characters")
        String username,

        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be a valid email address")
        @Size(max = 128, message = "email must be less than or equal to 128 characters")
        String email,

        @NotBlank(message = "displayName must not be blank")
        @Size(max = 128, message = "displayName must be less than or equal to 128 characters")
        String displayName,

        String initialPassword,

        @Size(max = 32, message = "credentialMode must be less than or equal to 32 characters")
        String credentialMode,

        @NotNull(message = "enabled must not be null")
        Boolean enabled,

        @NotEmpty(message = "roles must not be empty")
        Set<@NotBlank(message = "role must not be blank") String> roles
) {
}
