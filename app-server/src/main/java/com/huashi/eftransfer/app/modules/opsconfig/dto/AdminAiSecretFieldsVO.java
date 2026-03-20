package com.huashi.eftransfer.app.modules.opsconfig.dto;

public record AdminAiSecretFieldsVO(
        AdminAiSecretFieldVO chatApiKey,
        AdminAiSecretFieldVO embeddingApiKey,
        AdminAiSecretFieldVO rerankApiKey,
        AdminAiSecretFieldVO appServerInternalToken
) {
}
