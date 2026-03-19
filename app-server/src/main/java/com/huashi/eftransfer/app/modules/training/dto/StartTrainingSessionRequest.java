package com.huashi.eftransfer.app.modules.training.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StartTrainingSessionRequest(
        @NotNull(message = "planId must not be null")
        Long planId,
        @NotBlank(message = "mode must not be blank")
        String mode
) {
}
