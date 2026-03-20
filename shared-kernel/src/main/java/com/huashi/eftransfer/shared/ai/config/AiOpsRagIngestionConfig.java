package com.huashi.eftransfer.shared.ai.config;

public record AiOpsRagIngestionConfig(
        Integer exportPageSize,
        Integer embeddingBatchSize
) {
}
