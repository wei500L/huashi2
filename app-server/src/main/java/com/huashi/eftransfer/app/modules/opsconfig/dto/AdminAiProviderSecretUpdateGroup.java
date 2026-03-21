package com.huashi.eftransfer.app.modules.opsconfig.dto;

public record AdminAiProviderSecretUpdateGroup(
        AdminAiSecretValueUpdate chatApiKey,
        AdminAiSecretValueUpdate embeddingApiKey,
        AdminAiSecretValueUpdate rerankApiKey
) {
}
