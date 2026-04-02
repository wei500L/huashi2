package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("assessment_paper")
public class AssessmentPaperEntity extends BaseAuditEntity {

    @TableField("paper_code")
    private String paperCode;

    private String title;
    private String description;

    @TableField("owner_user_id")
    private Long ownerUserId;

    private String status;

    @TableField("duration_minutes")
    private Integer durationMinutes;

    @TableField("question_count")
    private Integer questionCount;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("latest_publish_at")
    private LocalDateTime latestPublishAt;

    public String getPaperCode() {
        return paperCode;
    }

    public void setPaperCode(String paperCode) {
        this.paperCode = paperCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public LocalDateTime getLatestPublishAt() {
        return latestPublishAt;
    }

    public void setLatestPublishAt(LocalDateTime latestPublishAt) {
        this.latestPublishAt = latestPublishAt;
    }
}
