package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("assessment_ai_analysis")
public class AssessmentAiAnalysisEntity extends BaseAuditEntity {
    @TableField("attempt_id") private Long attemptId;
    @TableField("metric_snapshot_id") private Long metricSnapshotId;
    @TableField("prompt_version") private String promptVersion;
    @TableField("idempotency_key") private String idempotencyKey;
    private String status;
    @TableField("retry_count") private Integer retryCount;
    @TableField("model_name") private String modelName;
    @TableField("prompt_tokens") private Integer promptTokens;
    @TableField("completion_tokens") private Integer completionTokens;
    @TableField("analysis_json") private String analysisJson;
    @TableField("rule_fallback_json") private String ruleFallbackJson;
    @TableField("raw_response") private String rawResponse;
    @TableField("fallback_reason") private String fallbackReason;
    @TableField("generation_record_id") private Long generationRecordId;
    @TableField("next_retry_at") private LocalDateTime nextRetryAt;
    @TableField("completed_at") private LocalDateTime completedAt;

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
    public Long getMetricSnapshotId() { return metricSnapshotId; }
    public void setMetricSnapshotId(Long metricSnapshotId) { this.metricSnapshotId = metricSnapshotId; }
    public String getPromptVersion() { return promptVersion; }
    public void setPromptVersion(String promptVersion) { this.promptVersion = promptVersion; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Integer getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
    public String getAnalysisJson() { return analysisJson; }
    public void setAnalysisJson(String analysisJson) { this.analysisJson = analysisJson; }
    public String getRuleFallbackJson() { return ruleFallbackJson; }
    public void setRuleFallbackJson(String ruleFallbackJson) { this.ruleFallbackJson = ruleFallbackJson; }
    public String getRawResponse() { return rawResponse; }
    public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
    public String getFallbackReason() { return fallbackReason; }
    public void setFallbackReason(String fallbackReason) { this.fallbackReason = fallbackReason; }
    public Long getGenerationRecordId() { return generationRecordId; }
    public void setGenerationRecordId(Long generationRecordId) { this.generationRecordId = generationRecordId; }
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
