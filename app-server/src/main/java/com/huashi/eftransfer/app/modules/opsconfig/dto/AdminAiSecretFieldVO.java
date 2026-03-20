package com.huashi.eftransfer.app.modules.opsconfig.dto;

public record AdminAiSecretFieldVO(
        boolean configured,
        String maskedValue
) {
}
