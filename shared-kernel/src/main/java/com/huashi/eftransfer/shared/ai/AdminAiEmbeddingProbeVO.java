package com.huashi.eftransfer.shared.ai;

import java.time.OffsetDateTime;

public record AdminAiEmbeddingProbeVO(
        boolean ok,
        String message,
        String provider,
        String model,
        long latencyMs,
        String providerRequestId,
        OffsetDateTime testedAt,
        Integer dimension,
        Integer expectedDimension,
        Integer itemCount,
        Double relatedSimilarity,
        Double unrelatedSimilarity,
        Double similarityMargin
) {
    public AdminAiEmbeddingProbeVO(
            boolean ok,
            String message,
            String provider,
            String model,
            long latencyMs,
            String providerRequestId,
            OffsetDateTime testedAt,
            Integer dimension,
            Integer expectedDimension,
            Integer itemCount
    ) {
        this(ok, message, provider, model, latencyMs, providerRequestId, testedAt,
                dimension, expectedDimension, itemCount, null, null, null);
    }
}
