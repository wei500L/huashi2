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
        Double similarityMargin,
        Double providerCompatibility,
        Integer providersChecked
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
            Integer itemCount,
            Double relatedSimilarity,
            Double unrelatedSimilarity,
            Double similarityMargin
    ) {
        this(ok, message, provider, model, latencyMs, providerRequestId, testedAt,
                dimension, expectedDimension, itemCount, relatedSimilarity, unrelatedSimilarity, similarityMargin, null, 1);
    }

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
                dimension, expectedDimension, itemCount, null, null, null, null, 1);
    }
}
