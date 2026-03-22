package com.huashi.eftransfer.app.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record AdminUserAccessUpdateRequest(
        @NotNull(message = "enabled must not be null")
        Boolean enabled,

        @NotEmpty(message = "roles must not be empty")
        Set<@NotBlank(message = "role must not be blank") String> roles
) {
}
