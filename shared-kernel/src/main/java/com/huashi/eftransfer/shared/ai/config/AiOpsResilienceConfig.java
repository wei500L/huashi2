package com.huashi.eftransfer.shared.ai.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AiOpsResilienceConfig(
        @NotNull(message = "maxAttempts is required")
        @Positive(message = "maxAttempts must be greater than 0")
        Integer maxAttempts,
        @NotBlank(message = "waitDuration is required")
        String waitDuration,
        @NotNull(message = "failureRateThreshold is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "failureRateThreshold must be between 0 and 100")
        @DecimalMax(value = "100.0", message = "failureRateThreshold must be between 0 and 100")
        Float failureRateThreshold,
        @NotNull(message = "slidingWindowSize is required")
        @Positive(message = "slidingWindowSize must be greater than 0")
        Integer slidingWindowSize,
        @NotBlank(message = "openStateDuration is required")
        String openStateDuration
) {
    @JsonCreator
    public AiOpsResilienceConfig(
            @JsonProperty("maxAttempts") Integer maxAttempts,
            @JsonProperty("waitDuration") String waitDuration,
            @JsonProperty("failureRateThreshold") Float failureRateThreshold,
            @JsonProperty("slidingWindowSize") Integer slidingWindowSize,
            @JsonProperty("openStateDuration") String openStateDuration,
            @JsonProperty("backoff") String legacyBackoff,
            @JsonProperty("waitDurationInOpenState") String legacyOpenStateDuration
    ) {
        this(
                maxAttempts,
                firstText(waitDuration, legacyBackoff),
                failureRateThreshold,
                slidingWindowSize,
                firstText(openStateDuration, legacyOpenStateDuration)
        );
    }

    private static String firstText(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred : fallback;
    }
}
