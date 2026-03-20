package com.huashi.eftransfer.app.modules.ai.vo;

import jakarta.validation.constraints.NotBlank;

public record AiRecommendationPathItemVO(
        @NotBlank(message = "title must not be blank")
        String title,
        @NotBlank(message = "reason must not be blank")
        String reason,
        @NotBlank(message = "priority must not be blank")
        String priority
) {
}
