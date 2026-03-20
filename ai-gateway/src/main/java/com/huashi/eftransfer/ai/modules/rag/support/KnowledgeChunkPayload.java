package com.huashi.eftransfer.ai.modules.rag.support;

import java.util.Map;

public record KnowledgeChunkPayload(
        String chunkKey,
        int chunkOrder,
        String sourceType,
        String sourceId,
        String title,
        String content,
        Map<String, Object> metadata,
        boolean active
) {
}
