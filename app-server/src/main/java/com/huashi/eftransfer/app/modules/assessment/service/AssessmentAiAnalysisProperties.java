package com.huashi.eftransfer.app.modules.assessment.service;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@Component("assessmentAiAnalysisProperties")
@ConfigurationProperties(prefix = "app.assessment.ai-analysis")
public class AssessmentAiAnalysisProperties {

    private boolean enabled = true;

    @NotNull
    private Duration pollInterval = Duration.ofSeconds(5);

    @Positive
    private int batchSize = 10;

    @Positive
    private int maxRetries = 2;

    @NotNull
    private Duration retryDelay = Duration.ofSeconds(5);

    @NotNull
    private Duration processingTimeout = Duration.ofMinutes(5);

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Duration getPollInterval() { return pollInterval; }
    public void setPollInterval(Duration pollInterval) { this.pollInterval = pollInterval; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public Duration getRetryDelay() { return retryDelay; }
    public void setRetryDelay(Duration retryDelay) { this.retryDelay = retryDelay; }
    public Duration getProcessingTimeout() { return processingTimeout; }
    public void setProcessingTimeout(Duration processingTimeout) { this.processingTimeout = processingTimeout; }
}
