package com.huashi.eftransfer.shared.ai.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AiOpsEmbeddingConfig(
        @NotBlank(message = "protocol is required")
        String protocol,
        @NotBlank(message = "baseUrl is required")
        String baseUrl,
        String apiKey,
        @NotBlank(message = "model is required")
        String model,
        String multimodalModel,
        @NotBlank(message = "connectTimeout is required")
        String connectTimeout,
        @NotBlank(message = "readTimeout is required")
        String readTimeout,
        @NotNull(message = "dimension is required")
        @Positive(message = "dimension must be greater than 0")
        Integer dimension
) {
    public AiOpsEmbeddingConfig(
            String protocol,
            String baseUrl,
            String apiKey,
            String model,
            String connectTimeout,
            String readTimeout,
            Integer dimension
    ) {
        this(protocol, baseUrl, apiKey, model, null, connectTimeout, readTimeout, dimension);
    }

    @JsonCreator
    public AiOpsEmbeddingConfig(
            @JsonProperty("protocol") String protocol,
            @JsonProperty("baseUrl") String baseUrl,
            @JsonProperty("apiKey") String apiKey,
            @JsonProperty("model") String model,
            @JsonProperty("multimodalModel") String multimodalModel,
            @JsonProperty("connectTimeout") String connectTimeout,
            @JsonProperty("readTimeout") String readTimeout,
            @JsonProperty("dimension") Integer dimension,
            @JsonProperty("timeout") String timeout
    ) {
        this(
                protocol,
                baseUrl,
                apiKey,
                model,
                multimodalModel,
                resolveTimeout(connectTimeout, readTimeout, timeout),
                resolveTimeout(readTimeout, connectTimeout, timeout),
                dimension
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
