package com.huashi.eftransfer.app.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompleteAccountActionRequest(
        @NotBlank(message = "password must not be blank")
        @Size(min = 8, max = 128, message = "password must be between 8 and 128 characters")
        String password
) {
}
