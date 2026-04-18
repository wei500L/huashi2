package com.huashi.eftransfer.shared.ai.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record AiOpsRerankConfig(
        @NotBlank(message = "protocol is required")
        String protocol,
        @NotBlank(message = "baseUrl is required")
        String baseUrl,
        String apiKey,
        @NotBlank(message = "model is required")
        String model,
        @NotBlank(message = "connectTimeout is required")
        String connectTimeout,
        @NotBlank(message = "readTimeout is required")
        String readTimeout
) {
    @JsonCreator
    public AiOpsRerankConfig(
            @JsonProperty("protocol") String protocol,
            @JsonProperty("baseUrl") String baseUrl,
            @JsonProperty("apiKey") String apiKey,
            @JsonProperty("model") String model,
            @JsonProperty("connectTimeout") String connectTimeout,
            @JsonProperty("readTimeout") String readTimeout,
            @JsonProperty("timeout") String timeout
    ) {
        this(
                protocol,
                baseUrl,
                apiKey,
                model,
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
