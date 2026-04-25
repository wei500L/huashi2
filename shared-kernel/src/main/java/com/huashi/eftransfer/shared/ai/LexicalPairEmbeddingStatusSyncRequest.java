package com.huashi.eftransfer.shared.ai;

import java.util.List;

public record LexicalPairEmbeddingStatusSyncRequest(
        List<LexicalPairEmbeddingStatusSyncItem> items
) {
}
