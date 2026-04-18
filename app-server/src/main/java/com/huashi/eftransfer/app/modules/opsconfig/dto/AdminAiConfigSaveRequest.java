package com.huashi.eftransfer.app.modules.opsconfig.dto;

import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record AdminAiConfigSaveRequest(
        @NotNull(message = "config is required")
        @Valid
        AiOpsConfigPayload config,
        String expectedVersion,
        Map<String, String> providerOrigins,
        @Valid
        AdminAiSecretUpdateGroup secrets
) {
}
