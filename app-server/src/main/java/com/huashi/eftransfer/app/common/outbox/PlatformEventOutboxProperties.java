package com.huashi.eftransfer.app.common.outbox;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@Component("platformEventOutboxProperties")
@ConfigurationProperties(prefix = "app.events.outbox")
public class PlatformEventOutboxProperties {

    private boolean enabled = true;

    @NotNull
    private Duration pollInterval = Duration.ofSeconds(5);

    @Positive
    private int batchSize = 20;

    @NotNull
    private Duration initialBackoff = Duration.ofSeconds(5);

    @NotNull
    private Duration maxBackoff = Duration.ofMinutes(5);

    @NotNull
    private Duration stuckThreshold = Duration.ofMinutes(1);

    @NotNull
    private Duration confirmTimeout = Duration.ofSeconds(5);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public Duration getInitialBackoff() {
        return initialBackoff;
    }

    public void setInitialBackoff(Duration initialBackoff) {
        this.initialBackoff = initialBackoff;
    }

    public Duration getMaxBackoff() {
        return maxBackoff;
    }

    public void setMaxBackoff(Duration maxBackoff) {
        this.maxBackoff = maxBackoff;
    }

    public Duration getStuckThreshold() {
        return stuckThreshold;
    }

    public void setStuckThreshold(Duration stuckThreshold) {
        this.stuckThreshold = stuckThreshold;
    }

    public Duration getConfirmTimeout() {
        return confirmTimeout;
    }

    public void setConfirmTimeout(Duration confirmTimeout) {
        this.confirmTimeout = confirmTimeout;
    }
}
