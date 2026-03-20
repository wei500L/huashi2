package com.huashi.eftransfer.app.modules.opsconfig.dto;

public record AdminAiSecretValueUpdate(
        Boolean retainExisting,
        String value
) {
}
