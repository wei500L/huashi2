package com.huashi.eftransfer.shared.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record AiOpsRagConfig(
        @NotNull(message = "appServer section is required")
        @Valid
        AiOpsRagAppServerConfig appServer,
        @NotNull(message = "ingestion section is required")
        @Valid
        AiOpsRagIngestionConfig ingestion,
        @NotNull(message = "retrieval section is required")
        @Valid
        AiOpsRagRetrievalConfig retrieval
) {
}
