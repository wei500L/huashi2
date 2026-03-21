package com.huashi.eftransfer.app.modules.training.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("review_schedule")
public class ReviewScheduleEntity extends BaseAuditEntity {

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("lexical_pair_id")
    private Long lexicalPairId;

    @TableField("wrong_book_id")
    private Long wrongBookId;

    @TableField("source_training_session_id")
    private Long sourceTrainingSessionId;

    @TableField("schedule_stage")
    private Integer scheduleStage;

    @TableField("interval_days")
    private Integer intervalDays;

    @TableField("due_at")
    private LocalDateTime dueAt;

    private String status;

    @TableField("review_mode")
    private String reviewMode;

    @TableField("trigger_reason")
    private String triggerReason;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("snapshot_json")
    private String snapshotJson;

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Long getLexicalPairId() {
        return lexicalPairId;
    }

    public void setLexicalPairId(Long lexicalPairId) {
        this.lexicalPairId = lexicalPairId;
    }

    public Long getWrongBookId() {
        return wrongBookId;
    }

    public void setWrongBookId(Long wrongBookId) {
        this.wrongBookId = wrongBookId;
    }

    public Long getSourceTrainingSessionId() {
        return sourceTrainingSessionId;
    }

    public void setSourceTrainingSessionId(Long sourceTrainingSessionId) {
        this.sourceTrainingSessionId = sourceTrainingSessionId;
    }

    public Integer getScheduleStage() {
        return scheduleStage;
    }

    public void setScheduleStage(Integer scheduleStage) {
        this.scheduleStage = scheduleStage;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewMode() {
        return reviewMode;
    }

    public void setReviewMode(String reviewMode) {
        this.reviewMode = reviewMode;
    }

    public String getTriggerReason() {
        return triggerReason;
    }

    public void setTriggerReason(String triggerReason) {
        this.triggerReason = triggerReason;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }
}
