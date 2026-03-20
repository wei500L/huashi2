package com.huashi.eftransfer.app.modules.ai.vo;

import jakarta.validation.constraints.NotBlank;

public record AiRecommendedTrainingModeVO(
        @NotBlank(message = "mode must not be blank")
        String mode,
        @NotBlank(message = "label must not be blank")
        String label,
        @NotBlank(message = "reason must not be blank")
        String reason
) {
}
