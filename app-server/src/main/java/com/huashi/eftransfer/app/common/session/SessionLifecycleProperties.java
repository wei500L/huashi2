package com.huashi.eftransfer.app.common.session;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@Component("sessionLifecycleProperties")
@ConfigurationProperties(prefix = "app.sessions.lifecycle")
public class SessionLifecycleProperties {

    private boolean enabled = true;

    @NotNull
    private Duration pollInterval = Duration.ofMinutes(15);

    @NotNull
    private Duration abandonTimeout = Duration.ofHours(12);

    @Positive
    private int batchSize = 200;

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

    public Duration getAbandonTimeout() {
        return abandonTimeout;
    }

    public void setAbandonTimeout(Duration abandonTimeout) {
        this.abandonTimeout = abandonTimeout;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
