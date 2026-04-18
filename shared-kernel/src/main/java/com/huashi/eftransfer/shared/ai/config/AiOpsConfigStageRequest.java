package com.huashi.eftransfer.shared.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AiOpsConfigStageRequest(
        @NotNull(message = "config is required")
        @Valid
        AiOpsConfigPayload config,
        String source,
        Long version
) {
}
