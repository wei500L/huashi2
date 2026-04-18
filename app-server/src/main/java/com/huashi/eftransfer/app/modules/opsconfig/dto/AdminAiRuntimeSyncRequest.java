package com.huashi.eftransfer.app.modules.opsconfig.dto;

import jakarta.validation.constraints.NotNull;

public record AdminAiRuntimeSyncRequest(
        @NotNull(message = "expectedVersion is required")
        Long expectedVersion
) {
}
