package com.huashi.eftransfer.app.modules.opsconfig.dto;

public record AdminAiProviderSecretFieldsVO(
        AdminAiSecretFieldVO chatApiKey,
        AdminAiSecretFieldVO embeddingApiKey,
        AdminAiSecretFieldVO rerankApiKey
) {
}
