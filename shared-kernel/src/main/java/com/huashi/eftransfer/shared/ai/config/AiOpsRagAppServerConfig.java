package com.huashi.eftransfer.shared.ai.config;

public record AiOpsRagAppServerConfig(
        String baseUrl,
        String internalToken,
        String connectTimeout,
        String readTimeout
) {
}
