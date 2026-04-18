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
        Double topScore
) {
}
