package com.huashi.eftransfer.app.modules.practice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

/**
 * A self-practice session: a student picks a bank (and optionally one of its
 * sections), the questions are snapshotted at session start and answered
 * without a timer. Sessions are purely self-service - no teacher publish,
 * no expiry.
 */
@TableName("practice_session")
public class PracticeSessionEntity extends BaseAuditEntity {

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("bank_code")
    private String bankCode;

    @TableField("section_code")
    private String sectionCode;

    private String status;

    @TableField("total_count")
    private Integer totalCount;

    @TableField("answered_count")
    private Integer answeredCount;

    @TableField("correct_count")
    private Integer correctCount;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("tutoring_status")
    private String tutoringStatus;

    @TableField("tutoring_json")
    private String tutoringJson;

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getSectionCode() {
        return sectionCode;
    }

    public void setSectionCode(String sectionCode) {
        this.sectionCode = sectionCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

    public Integer getAnsweredCount() {
        return answeredCount;
    }

    public void setAnsweredCount(Integer answeredCount) {
        this.answeredCount = answeredCount;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
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

    public String getTutoringStatus() {
        return tutoringStatus;
    }

    public void setTutoringStatus(String tutoringStatus) {
        this.tutoringStatus = tutoringStatus;
    }

    public String getTutoringJson() {
        return tutoringJson;
    }

    public void setTutoringJson(String tutoringJson) {
        this.tutoringJson = tutoringJson;
    }
}
