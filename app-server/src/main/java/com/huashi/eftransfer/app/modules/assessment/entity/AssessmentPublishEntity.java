package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("assessment_publish")
public class AssessmentPublishEntity extends BaseAuditEntity {

    @TableField("paper_id")
    private Long paperId;

    @TableField("teaching_class_id")
    private Long teachingClassId;

    @TableField("delivery_mode")
    private String deliveryMode;

    @TableField("published_by")
    private Long publishedBy;

    private String status;

    @TableField("paper_title_snapshot")
    private String paperTitleSnapshot;

    @TableField("paper_description_snapshot")
    private String paperDescriptionSnapshot;

    @TableField("question_count_snapshot")
    private Integer questionCountSnapshot;

    @TableField("total_score_snapshot")
    private Integer totalScoreSnapshot;

    @TableField("duration_minutes")
    private Integer durationMinutes;

    @TableField("instructions_text")
    private String instructionsText;

    @TableField("starts_at")
    private LocalDateTime startsAt;

    @TableField("due_at")
    private LocalDateTime dueAt;

    @TableField("result_release_policy")
    private String resultReleasePolicy;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    public Long getPaperId() {
        return paperId;
    }

    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

    public Long getTeachingClassId() {
        return teachingClassId;
    }

    public void setTeachingClassId(Long teachingClassId) {
        this.teachingClassId = teachingClassId;
    }

    public String getDeliveryMode() {
        return deliveryMode;
    }

    public void setDeliveryMode(String deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    public Long getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(Long publishedBy) {
        this.publishedBy = publishedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaperTitleSnapshot() {
        return paperTitleSnapshot;
    }

    public void setPaperTitleSnapshot(String paperTitleSnapshot) {
        this.paperTitleSnapshot = paperTitleSnapshot;
    }

    public String getPaperDescriptionSnapshot() {
        return paperDescriptionSnapshot;
    }

    public void setPaperDescriptionSnapshot(String paperDescriptionSnapshot) {
        this.paperDescriptionSnapshot = paperDescriptionSnapshot;
    }

    public Integer getQuestionCountSnapshot() {
        return questionCountSnapshot;
    }

    public void setQuestionCountSnapshot(Integer questionCountSnapshot) {
        this.questionCountSnapshot = questionCountSnapshot;
    }

    public Integer getTotalScoreSnapshot() {
        return totalScoreSnapshot;
    }

    public void setTotalScoreSnapshot(Integer totalScoreSnapshot) {
        this.totalScoreSnapshot = totalScoreSnapshot;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getInstructionsText() {
        return instructionsText;
    }

    public void setInstructionsText(String instructionsText) {
        this.instructionsText = instructionsText;
    }

    public LocalDateTime getStartsAt() {
        return startsAt;
    }

    public void setStartsAt(LocalDateTime startsAt) {
        this.startsAt = startsAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public String getResultReleasePolicy() {
        return resultReleasePolicy;
    }

    public void setResultReleasePolicy(String resultReleasePolicy) {
        this.resultReleasePolicy = resultReleasePolicy;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
