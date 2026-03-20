package com.huashi.eftransfer.app.modules.opsconfig.support;

import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;

import java.time.OffsetDateTime;

public record StoredAiOpsConfig(
        AiOpsConfigPayload config,
        Long version,
        OffsetDateTime updatedAt
) {
}
