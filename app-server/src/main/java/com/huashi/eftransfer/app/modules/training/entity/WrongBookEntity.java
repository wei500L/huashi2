package com.huashi.eftransfer.app.modules.training.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.shared.model.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("wrong_book")
public class WrongBookEntity extends BaseAuditEntity {

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("lexical_pair_id")
    private Long lexicalPairId;

    @TableField("source_training_session_id")
    private Long sourceTrainingSessionId;

    @TableField("source_item_result_id")
    private Long sourceItemResultId;

    @TableField("wrong_count")
    private Integer wrongCount;

    @TableField("first_wrong_at")
    private LocalDateTime firstWrongAt;

    @TableField("last_wrong_at")
    private LocalDateTime lastWrongAt;

    @TableField("last_error_type")
    private String lastErrorType;

    @TableField("mastery_status")
    private String masteryStatus;

    @TableField("next_review_at")
    private LocalDateTime nextReviewAt;

    @TableField("latest_snapshot_json")
    private String latestSnapshotJson;

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

    public Long getSourceTrainingSessionId() {
        return sourceTrainingSessionId;
    }

    public void setSourceTrainingSessionId(Long sourceTrainingSessionId) {
        this.sourceTrainingSessionId = sourceTrainingSessionId;
    }

    public Long getSourceItemResultId() {
        return sourceItemResultId;
    }

    public void setSourceItemResultId(Long sourceItemResultId) {
        this.sourceItemResultId = sourceItemResultId;
    }

    public Integer getWrongCount() {
        return wrongCount;
    }

    public void setWrongCount(Integer wrongCount) {
        this.wrongCount = wrongCount;
    }

    public LocalDateTime getFirstWrongAt() {
        return firstWrongAt;
    }

    public void setFirstWrongAt(LocalDateTime firstWrongAt) {
        this.firstWrongAt = firstWrongAt;
    }

    public LocalDateTime getLastWrongAt() {
        return lastWrongAt;
    }

    public void setLastWrongAt(LocalDateTime lastWrongAt) {
        this.lastWrongAt = lastWrongAt;
    }

    public String getLastErrorType() {
        return lastErrorType;
    }

    public void setLastErrorType(String lastErrorType) {
        this.lastErrorType = lastErrorType;
    }

    public String getMasteryStatus() {
        return masteryStatus;
    }

    public void setMasteryStatus(String masteryStatus) {
        this.masteryStatus = masteryStatus;
    }

    public LocalDateTime getNextReviewAt() {
        return nextReviewAt;
    }

    public void setNextReviewAt(LocalDateTime nextReviewAt) {
        this.nextReviewAt = nextReviewAt;
    }

    public String getLatestSnapshotJson() {
        return latestSnapshotJson;
    }

    public void setLatestSnapshotJson(String latestSnapshotJson) {
        this.latestSnapshotJson = latestSnapshotJson;
    }
}
