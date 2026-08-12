package com.huashi.eftransfer.shared.ai;

import java.time.OffsetDateTime;
import java.util.List;

public record PracticeWordKnowledgeExportPageResponse(
        List<PracticeWordKnowledgeExportItem> items,
        String nextCursor,
        OffsetDateTime generatedAt
) {
    public PracticeWordKnowledgeExportPageResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
