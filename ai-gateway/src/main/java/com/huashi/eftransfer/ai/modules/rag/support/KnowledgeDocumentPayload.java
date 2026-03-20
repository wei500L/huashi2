package com.huashi.eftransfer.ai.modules.rag.support;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public record KnowledgeDocumentPayload(
        String sourceType,
        String sourceId,
        String title,
        OffsetDateTime sourceUpdatedAt,
        boolean active,
        Map<String, Object> metadata,
        List<KnowledgeChunkPayload> chunks
) {
}
