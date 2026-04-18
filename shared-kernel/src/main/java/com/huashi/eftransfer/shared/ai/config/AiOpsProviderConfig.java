package com.huashi.eftransfer.shared.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Map;

public record AiOpsProviderConfig(
        @NotBlank(message = "activeProvider is required")
        String activeProvider,
        @NotBlank(message = "fallbackProvider is required")
        String fallbackProvider,
        @NotEmpty(message = "at least one provider definition is required")
        Map<String, @Valid AiOpsProviderDefinition> providers
) {
}
