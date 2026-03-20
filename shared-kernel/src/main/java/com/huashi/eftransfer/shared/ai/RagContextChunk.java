package com.huashi.eftransfer.shared.ai;

import java.util.Map;

public record RagContextChunk(
        String citationId,
        String sourceType,
        String sourceId,
        String title,
        String content,
        String snippet,
        Double score,
        Map<String, Object> metadata
) {
}
