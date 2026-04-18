package com.huashi.eftransfer.app.modules.assessment.service;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@Component("assessmentTimeoutProperties")
@ConfigurationProperties(prefix = "app.assessment.timeout")
public class AssessmentTimeoutProperties {

    private boolean enabled = true;

    @NotNull
    private Duration pollInterval = Duration.ofSeconds(30);

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

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
