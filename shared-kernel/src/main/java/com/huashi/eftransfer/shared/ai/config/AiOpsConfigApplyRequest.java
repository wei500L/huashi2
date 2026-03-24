package com.huashi.eftransfer.shared.ai.config;

public record AiOpsConfigApplyRequest(
        AiOpsConfigPayload config,
        String source,
        Long version
) {
}
