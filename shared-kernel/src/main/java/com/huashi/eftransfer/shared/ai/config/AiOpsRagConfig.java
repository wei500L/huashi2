package com.huashi.eftransfer.shared.ai.config;

public record AiOpsRagConfig(
        AiOpsRagAppServerConfig appServer,
        AiOpsRagIngestionConfig ingestion,
        AiOpsRagRetrievalConfig retrieval
) {
}
