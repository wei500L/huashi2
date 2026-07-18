package com.huashi.eftransfer.shared.ai.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiOpsRerankConfig(
        @NotBlank(message = "protocol is required")
        String protocol,
        @NotBlank(message = "baseUrl is required")
        String baseUrl,
        String apiKey,
        @NotBlank(message = "model is required")
        @Size(max = 128, message = "model must be less than or equal to 128 characters")
        String model,
        @Size(max = 128, message = "multimodalModel must be less than or equal to 128 characters")
        String multimodalModel,
        @NotBlank(message = "connectTimeout is required")
        String connectTimeout,
        @NotBlank(message = "readTimeout is required")
        String readTimeout
) {
    public AiOpsRerankConfig(
            String protocol,
            String baseUrl,
            String apiKey,
            String model,
            String connectTimeout,
            String readTimeout
    ) {
        this(protocol, baseUrl, apiKey, model, null, connectTimeout, readTimeout);
    }

    @JsonCreator
    public AiOpsRerankConfig(
            @JsonProperty("protocol") String protocol,
            @JsonProperty("baseUrl") String baseUrl,
            @JsonProperty("apiKey") String apiKey,
            @JsonProperty("model") String model,
            @JsonProperty("multimodalModel") String multimodalModel,
            @JsonProperty("connectTimeout") String connectTimeout,
            @JsonProperty("readTimeout") String readTimeout,
            @JsonProperty("timeout") String timeout
    ) {
        this(
                protocol,
                baseUrl,
                apiKey,
                model,
                multimodalModel,
                resolveTimeout(connectTimeout, readTimeout, timeout),
                resolveTimeout(readTimeout, connectTimeout, timeout)
        );
    }

    private static String resolveTimeout(String preferred, String secondary, String legacyTimeout) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        if (secondary != null && !secondary.isBlank()) {
            return secondary;
        }
        return legacyTimeout;
    }
}
