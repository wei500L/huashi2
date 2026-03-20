package com.huashi.eftransfer.ai.modules.rag.support;

import java.util.Map;

public record RagRetrievedChunk(
        Long chunkId,
        String citationId,
        String sourceType,
        String sourceId,
        String title,
        String content,
        String snippet,
        double score,
        Map<String, Object> metadata
) {
}
