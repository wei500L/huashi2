package com.huashi.eftransfer.shared.ai.config;

import jakarta.validation.constraints.NotBlank;

public record AiOpsRerankConfig(
        @NotBlank(message = "protocol is required")
        String protocol,
        @NotBlank(message = "baseUrl is required")
        String baseUrl,
        String apiKey,
        @NotBlank(message = "model is required")
        String model,
        @NotBlank(message = "connectTimeout is required")
        String connectTimeout,
        @NotBlank(message = "readTimeout is required")
        String readTimeout
) {
}
