package com.huashi.eftransfer.app.modules.opsconfig.dto;

import java.time.OffsetDateTime;

public record AdminOutboxRecordVO(
        Long id,
        String eventId,
        String eventType,
        String routingKey,
        String status,
        int attemptCount,
        String traceId,
        String lastError,
        OffsetDateTime nextAttemptAt,
        OffsetDateTime processingStartedAt,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
