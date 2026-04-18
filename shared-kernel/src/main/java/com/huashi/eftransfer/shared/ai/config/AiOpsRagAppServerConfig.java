package com.huashi.eftransfer.shared.ai.config;

import jakarta.validation.constraints.NotBlank;

public record AiOpsRagAppServerConfig(
        @NotBlank(message = "baseUrl is required")
        String baseUrl,
        String internalToken,
        @NotBlank(message = "connectTimeout is required")
        String connectTimeout,
        @NotBlank(message = "readTimeout is required")
        String readTimeout
) {
}
