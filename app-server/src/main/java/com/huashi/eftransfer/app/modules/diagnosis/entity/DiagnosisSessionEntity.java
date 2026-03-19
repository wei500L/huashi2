package com.huashi.eftransfer.app.modules.diagnosis.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.shared.model.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("diagnosis_session")
public class DiagnosisSessionEntity extends BaseAuditEntity {

    @TableField("template_id")
    private Long templateId;

    @TableField("owner_user_id")
    private Long ownerUserId;

    private String status;

    @TableField("session_seed")
    private Long sessionSeed;

    @TableField("total_items")
    private Integer totalItems;

    @TableField("answered_items")
    private Integer answeredItems;

    @TableField("current_item_order")
    private Integer currentItemOrder;

    @TableField("last_saved_at")
    private LocalDateTime lastSavedAt;

    @TableField("progress_snapshot_json")
    private String progressSnapshotJson;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
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

    public LocalDateTime getLastSavedAt() {
        return lastSavedAt;
    }

    public void setLastSavedAt(LocalDateTime lastSavedAt) {
        this.lastSavedAt = lastSavedAt;
    }

    public String getProgressSnapshotJson() {
        return progressSnapshotJson;
    }

    public void setProgressSnapshotJson(String progressSnapshotJson) {
        this.progressSnapshotJson = progressSnapshotJson;
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
