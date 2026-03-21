package com.huashi.eftransfer.app.common.outbox;

public record PlatformOutboxMessage(
        String eventId,
        String eventType,
        String exchangeName,
        String routingKey,
        String payloadJson,
        String headersJson,
        String traceId
) {
}
