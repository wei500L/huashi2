package com.huashi.eftransfer.ai.modules.rag.support;

import java.util.Map;

public record KnowledgeSearchCandidate(
        Long chunkId,
        String sourceType,
        String sourceId,
        String title,
        String content,
        Map<String, Object> metadata,
        double similarityScore
) {
}
