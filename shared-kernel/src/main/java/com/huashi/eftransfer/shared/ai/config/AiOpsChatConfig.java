package com.huashi.eftransfer.shared.ai.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AiOpsChatConfig(
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
        String readTimeout,
        @NotNull(message = "temperature is required")
        @DecimalMin(value = "0.0", message = "temperature must be between 0 and 2")
        @DecimalMax(value = "2.0", message = "temperature must be between 0 and 2")
        Double temperature,
        @NotNull(message = "maxTokens is required")
        @Positive(message = "maxTokens must be greater than 0")
        Integer maxTokens
) {
    @JsonCreator
    public AiOpsChatConfig(
            @JsonProperty("protocol") String protocol,
            @JsonProperty("baseUrl") String baseUrl,
            @JsonProperty("apiKey") String apiKey,
            @JsonProperty("model") String model,
            @JsonProperty("connectTimeout") String connectTimeout,
            @JsonProperty("readTimeout") String readTimeout,
            @JsonProperty("temperature") Double temperature,
            @JsonProperty("maxTokens") Integer maxTokens,
            @JsonProperty("timeout") String timeout
    ) {
        this(
                protocol,
                baseUrl,
                apiKey,
                model,
                resolveTimeout(connectTimeout, readTimeout, timeout),
                resolveTimeout(readTimeout, connectTimeout, timeout),
                temperature,
                maxTokens
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
