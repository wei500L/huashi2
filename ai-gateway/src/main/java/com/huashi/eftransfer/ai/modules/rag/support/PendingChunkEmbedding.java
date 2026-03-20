package com.huashi.eftransfer.ai.modules.rag.support;

public record PendingChunkEmbedding(
        Long chunkId,
        String content,
        String contentHash
) {
}
