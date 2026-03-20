package com.huashi.eftransfer.shared.ai;

import java.time.OffsetDateTime;
import java.util.List;

public record LexicalKnowledgeExportPageResponse(
        List<LexicalKnowledgeExportItem> items,
        String nextCursor,
        OffsetDateTime watermark
) {
}
