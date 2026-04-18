package com.huashi.eftransfer.app.modules.opsconfig.support;

import java.time.OffsetDateTime;

public record AiRuntimeSyncOutboxPayload(
        Long targetVersion,
        Long actorUserId,
        OffsetDateTime requestedAt
) {
}
