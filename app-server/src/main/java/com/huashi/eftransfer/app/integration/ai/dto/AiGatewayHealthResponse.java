package com.huashi.eftransfer.app.integration.ai.dto;

import java.time.OffsetDateTime;

public record AiGatewayHealthResponse(
        String service,
        String status,
        String provider,
        String fallbackProvider,
        String chatModel,
        String embeddingModel,
        String rerankModel,
        boolean databaseReady,
        boolean vectorStoreReady,
        boolean providerReady,
        boolean rerankReady,
        boolean appServerReady,
        String vectorExtensionVersion,
        java.util.List<String> activeProfiles,
        OffsetDateTime timestamp,
        String appServerError
) {
}
