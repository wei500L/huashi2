package com.huashi.eftransfer.app.modules.ai.dto;

import jakarta.validation.constraints.NotNull;

public record PracticeTutoringRequest(
        @NotNull(message = "practiceSessionId must not be null")
        Long practiceSessionId
) {
}
