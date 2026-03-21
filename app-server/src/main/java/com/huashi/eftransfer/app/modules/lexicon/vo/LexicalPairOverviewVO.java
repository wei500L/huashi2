package com.huashi.eftransfer.app.modules.lexicon.vo;

import java.time.LocalDateTime;

public record LexicalPairOverviewVO(
        long totalCount,
        long activeCount,
        long pendingEmbeddingCount,
        long embeddedCount,
        long failedEmbeddingCount,
        LocalDateTime latestCreatedAt,
        LocalDateTime latestUpdatedAt,
        LocalDateTime latestEmbeddedAt
) {
}
