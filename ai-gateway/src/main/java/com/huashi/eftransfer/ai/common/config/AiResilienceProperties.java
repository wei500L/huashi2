package com.huashi.eftransfer.ai.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "ai.resilience")
public class AiResilienceProperties {

    private int maxAttempts = 3;

    private Duration waitDuration = Duration.ofMillis(500);

    private float failureRateThreshold = 50.0F;

    private int slidingWindowSize = 20;

    private Duration openStateDuration = Duration.ofSeconds(30);

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getWaitDuration() {
        return waitDuration;
    }

    public void setWaitDuration(Duration waitDuration) {
        this.waitDuration = waitDuration;
    }

    public float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public void setFailureRateThreshold(float failureRateThreshold) {
        this.failureRateThreshold = failureRateThreshold;
    }

    public int getSlidingWindowSize() {
        return slidingWindowSize;
    }

    public void setSlidingWindowSize(int slidingWindowSize) {
        this.slidingWindowSize = slidingWindowSize;
    }

    public Duration getOpenStateDuration() {
        return openStateDuration;
    }

    public void setOpenStateDuration(Duration openStateDuration) {
        this.openStateDuration = openStateDuration;
    }
}
