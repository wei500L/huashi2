package com.huashi.eftransfer.shared.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChatMessage(
        @NotBlank(message = "role must not be blank")
        @Pattern(
                regexp = "system|user|assistant",
                message = "role must be one of system, user, assistant"
        )
        String role,
        @NotBlank(message = "content must not be blank")
        String content
) {
}
