package com.huashi.eftransfer.shared.ai.config;

import jakarta.validation.constraints.NotBlank;

public record AiOpsConfigCommitRequest(
        @NotBlank(message = "stageId is required")
        String stageId
) {
}
