package com.huashi.eftransfer.ai.modules.health.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record AiHealthPayload(
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
        String vectorExtensionVersion,
        List<String> activeProfiles,
        OffsetDateTime timestamp
) {
}
