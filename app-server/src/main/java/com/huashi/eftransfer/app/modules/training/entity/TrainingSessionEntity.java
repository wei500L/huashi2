package com.huashi.eftransfer.app.modules.training.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.shared.model.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("training_session")
public class TrainingSessionEntity extends BaseAuditEntity {

    @TableField("plan_id")
    private Long planId;

    @TableField("owner_user_id")
    private Long ownerUserId;

    private String mode;
    private String status;

    @TableField("session_seed")
    private Long sessionSeed;

    @TableField("total_items")
    private Integer totalItems;

    @TableField("answered_items")
    private Integer answeredItems;

    @TableField("current_item_order")
    private Integer currentItemOrder;

    @TableField("planned_difficulty")
    private Integer plannedDifficulty;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("progress_snapshot_json")
    private String progressSnapshotJson;

    @TableField("summary_snapshot_json")
    private String summarySnapshotJson;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getSessionSeed() {
        return sessionSeed;
    }

    public void setSessionSeed(Long sessionSeed) {
        this.sessionSeed = sessionSeed;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public Integer getAnsweredItems() {
        return answeredItems;
    }

    public void setAnsweredItems(Integer answeredItems) {
        this.answeredItems = answeredItems;
    }

    public Integer getCurrentItemOrder() {
        return currentItemOrder;
    }

    public void setCurrentItemOrder(Integer currentItemOrder) {
        this.currentItemOrder = currentItemOrder;
    }

    public Integer getPlannedDifficulty() {
        return plannedDifficulty;
    }

    public void setPlannedDifficulty(Integer plannedDifficulty) {
        this.plannedDifficulty = plannedDifficulty;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getProgressSnapshotJson() {
        return progressSnapshotJson;
    }

    public void setProgressSnapshotJson(String progressSnapshotJson) {
        this.progressSnapshotJson = progressSnapshotJson;
    }

    public String getSummarySnapshotJson() {
        return summarySnapshotJson;
    }

    public void setSummarySnapshotJson(String summarySnapshotJson) {
        this.summarySnapshotJson = summarySnapshotJson;
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
