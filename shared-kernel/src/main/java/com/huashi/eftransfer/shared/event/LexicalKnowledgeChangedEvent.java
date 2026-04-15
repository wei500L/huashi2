package com.huashi.eftransfer.shared.event;

import java.time.OffsetDateTime;
import java.util.List;

public record LexicalKnowledgeChangedEvent(
        String eventId,
        Integer eventVersion,
        String sourceType,
        List<Long> sourceIds,
        OffsetDateTime occurredAt,
        String traceId
) {
}
