package com.huashi.eftransfer.shared.ai;

import java.time.OffsetDateTime;

public record LexicalPairEmbeddingStatusSyncItem(
        Long lexicalPairId,
        String embeddingStatus,
        OffsetDateTime lastEmbeddedAt
) {
}
