package com.huashi.eftransfer.shared.ai;

public record RagCitation(
        String citationId,
        String sourceType,
        String sourceId,
        String title,
        String snippet,
        Double score
) {
}
