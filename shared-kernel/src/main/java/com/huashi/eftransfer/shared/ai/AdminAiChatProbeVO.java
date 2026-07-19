package com.huashi.eftransfer.shared.ai;

import java.time.OffsetDateTime;

public record AdminAiChatProbeVO(
        boolean ok,
        String message,
        String provider,
        String model,
        String protocol,
        long latencyMs,
        String providerRequestId,
        OffsetDateTime testedAt,
        boolean structuredOutputValid,
        Integer providersChecked
) {
}
