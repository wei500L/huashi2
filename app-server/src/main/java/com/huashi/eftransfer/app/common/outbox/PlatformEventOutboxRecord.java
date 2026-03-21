package com.huashi.eftransfer.app.common.outbox;

import java.time.OffsetDateTime;

public record PlatformEventOutboxRecord(
        Long id,
        String eventId,
        String eventType,
        String exchangeName,
        String routingKey,
        String payloadJson,
        String headersJson,
        String traceId,
        PlatformEventOutboxStatus status,
        int attemptCount,
        OffsetDateTime nextAttemptAt,
        String lastError,
        OffsetDateTime processingStartedAt,
        OffsetDateTime publishedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
