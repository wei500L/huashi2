package com.huashi.eftransfer.app.integration.ai.dto;

import java.time.OffsetDateTime;

public record AiGatewayHealthResponse(
        String service,
        String status,
        String provider,
        boolean databaseReady,
        boolean vectorStoreReady,
        boolean providerReady,
        OffsetDateTime timestamp
) {
}
