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
        Integer itemCount
) {
}
