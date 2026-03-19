package com.huashi.eftransfer.app.modules.training.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.shared.model.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("training_plan")
public class TrainingPlanEntity extends BaseAuditEntity {

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("source_diagnosis_session_id")
    private Long sourceDiagnosisSessionId;

    @TableField("source_diagnosis_summary_id")
    private Long sourceDiagnosisSummaryId;

    private String status;

    @TableField("priority_mode")
    private String priorityMode;

    @TableField("recommended_difficulty")
    private Integer recommendedDifficulty;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("estimated_training_volume")
    private Integer estimatedTrainingVolume;

    @TableField("recommendation_reason")
    private String recommendationReason;

    @TableField("target_metrics_json")
    private String targetMetricsJson;

    @TableField("generated_at")
    private LocalDateTime generatedAt;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Long getSourceDiagnosisSessionId() {
        return sourceDiagnosisSessionId;
    }

    public void setSourceDiagnosisSessionId(Long sourceDiagnosisSessionId) {
        this.sourceDiagnosisSessionId = sourceDiagnosisSessionId;
    }

    public Long getSourceDiagnosisSummaryId() {
        return sourceDiagnosisSummaryId;
    }

    public void setSourceDiagnosisSummaryId(Long sourceDiagnosisSummaryId) {
        this.sourceDiagnosisSummaryId = sourceDiagnosisSummaryId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriorityMode() {
        return priorityMode;
    }

    public void setPriorityMode(String priorityMode) {
        this.priorityMode = priorityMode;
    }

    public Integer getRecommendedDifficulty() {
        return recommendedDifficulty;
    }

    public void setRecommendedDifficulty(Integer recommendedDifficulty) {
        this.recommendedDifficulty = recommendedDifficulty;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Integer getEstimatedTrainingVolume() {
        return estimatedTrainingVolume;
    }

    public void setEstimatedTrainingVolume(Integer estimatedTrainingVolume) {
        this.estimatedTrainingVolume = estimatedTrainingVolume;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public void setRecommendationReason(String recommendationReason) {
        this.recommendationReason = recommendationReason;
    }

    public String getTargetMetricsJson() {
        return targetMetricsJson;
    }

    public void setTargetMetricsJson(String targetMetricsJson) {
        this.targetMetricsJson = targetMetricsJson;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
