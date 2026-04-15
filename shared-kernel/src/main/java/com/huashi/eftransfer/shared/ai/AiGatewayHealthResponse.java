package com.huashi.eftransfer.shared.ai;

import java.time.OffsetDateTime;
import java.util.List;

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
        List<String> activeProfiles,
        OffsetDateTime timestamp,
        String appServerError
) {
}
