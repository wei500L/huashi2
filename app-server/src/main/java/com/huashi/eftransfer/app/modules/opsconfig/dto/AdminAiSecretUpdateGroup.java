package com.huashi.eftransfer.app.modules.opsconfig.dto;

public record AdminAiSecretUpdateGroup(
        AdminAiSecretValueUpdate chatApiKey,
        AdminAiSecretValueUpdate embeddingApiKey,
        AdminAiSecretValueUpdate rerankApiKey,
        AdminAiSecretValueUpdate appServerInternalToken
) {
}
