package com.huashi.eftransfer.app.modules.assessment.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@Component("researchAnalyticsProperties")
@ConfigurationProperties(prefix = "app.assessment.research")
public class ResearchAnalyticsProperties {

    @Positive
    private int minSampleSize = 5;

    @NotBlank
    private String statisticsVersion = "RESEARCH_STATS_V1";

    @NotBlank
    private String promptVersion = "research-aggregate-report/v1";

    private boolean aiEnabled = true;

    @NotNull
    private Duration pollInterval = Duration.ofSeconds(5);

    @Positive
    private int batchSize = 5;

    @Positive
    private int maxRetries = 2;

    @NotNull
    private Duration retryDelay = Duration.ofSeconds(5);

    @NotNull
    private Duration processingTimeout = Duration.ofMinutes(5);

    @Positive
    private long maxFileBytes = 20L * 1024 * 1024;

    @Positive
    private int maxFilesPerQuestion = 5;

    @Positive
    private long maxAttemptBytes = 50L * 1024 * 1024;

    @NotBlank
    private String storageProvider = "LOCAL";

    @NotBlank
    private String localRoot = System.getProperty("java.io.tmpdir") + "/huashi-research-files";

    @NotNull
    private Duration orphanAfter = Duration.ofHours(24);

    @NotNull
    private Duration retention = Duration.ofDays(730);

    public int getMinSampleSize() { return minSampleSize; }
    public void setMinSampleSize(int minSampleSize) { this.minSampleSize = minSampleSize; }
    public String getStatisticsVersion() { return statisticsVersion; }
    public void setStatisticsVersion(String statisticsVersion) { this.statisticsVersion = statisticsVersion; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public boolean isAiEnabled() { return aiEnabled; }
    public void setAiEnabled(boolean aiEnabled) { this.aiEnabled = aiEnabled; }
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
    public long getMaxFileBytes() { return maxFileBytes; }
    public void setMaxFileBytes(long maxFileBytes) { this.maxFileBytes = maxFileBytes; }
    public int getMaxFilesPerQuestion() { return maxFilesPerQuestion; }
    public void setMaxFilesPerQuestion(int maxFilesPerQuestion) { this.maxFilesPerQuestion = maxFilesPerQuestion; }
    public long getMaxAttemptBytes() { return maxAttemptBytes; }
    public void setMaxAttemptBytes(long maxAttemptBytes) { this.maxAttemptBytes = maxAttemptBytes; }
    public String getStorageProvider() { return storageProvider; }
    public void setStorageProvider(String storageProvider) { this.storageProvider = storageProvider; }
    public String getLocalRoot() { return localRoot; }
    public void setLocalRoot(String localRoot) { this.localRoot = localRoot; }
    public Duration getOrphanAfter() { return orphanAfter; }
    public void setOrphanAfter(Duration orphanAfter) { this.orphanAfter = orphanAfter; }
    public Duration getRetention() { return retention; }
    public void setRetention(Duration retention) { this.retention = retention; }
}
