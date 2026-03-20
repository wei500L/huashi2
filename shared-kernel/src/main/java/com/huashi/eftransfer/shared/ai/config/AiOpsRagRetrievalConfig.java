package com.huashi.eftransfer.shared.ai.config;

public record AiOpsRagRetrievalConfig(
        Integer recallTopK,
        Double recallThreshold,
        Integer rerankTopN,
        Double rerankThreshold,
        Integer finalTopK
) {
}
