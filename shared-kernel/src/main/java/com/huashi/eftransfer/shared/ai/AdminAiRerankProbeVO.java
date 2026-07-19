package com.huashi.eftransfer.shared.ai;

import java.time.OffsetDateTime;

public record AdminAiRerankProbeVO(
        boolean ok,
        String message,
        String provider,
        String model,
        long latencyMs,
        String providerRequestId,
        OffsetDateTime testedAt,
        Integer documentsCount,
        Integer returnedCount,
        boolean ordered,
        Integer topDocumentIndex,
        Double topScore,
        Integer providersChecked
) {
    public AdminAiRerankProbeVO(
            boolean ok,
            String message,
            String provider,
            String model,
            long latencyMs,
            String providerRequestId,
            OffsetDateTime testedAt,
            Integer documentsCount,
            Integer returnedCount,
            boolean ordered,
            Integer topDocumentIndex,
            Double topScore
    ) {
        this(ok, message, provider, model, latencyMs, providerRequestId, testedAt,
                documentsCount, returnedCount, ordered, topDocumentIndex, topScore, 1);
    }
}
