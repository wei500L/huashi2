package com.huashi.eftransfer.shared.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChatMessage(
        @NotBlank(message = "role must not be blank")
        @Pattern(
                regexp = "system|user|assistant",
                message = "role must be one of system, user, assistant"
        )
        String role,
        @NotBlank(message = "content must not be blank")
        @Size(max = 131072, message = "message content is too long")
        String content
) {
}
