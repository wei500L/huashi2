package com.huashi.eftransfer.app.modules.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("learning_profile_snapshot")
public class LearningProfileSnapshotEntity extends BaseAuditEntity {

    private String scope;

    @TableField("student_user_id")
    private Long studentUserId;

    @TableField("teaching_class_id")
    private Long teachingClassId;

    @TableField("teacher_user_id")
    private Long teacherUserId;

    @TableField("last_diagnosis_summary_id")
    private Long lastDiagnosisSummaryId;

    @TableField("last_training_session_id")
    private Long lastTrainingSessionId;

    @TableField("primary_risk_level")
    private String primaryRiskLevel;

    @TableField("recommended_training_mode")
    private String recommendedTrainingMode;

    @TableField("pending_review_count")
    private Integer pendingReviewCount;

    @TableField("high_risk_pair_count")
    private Integer highRiskPairCount;

    @TableField("recent_accuracy")
    private BigDecimal recentAccuracy;

    @TableField("recent_negative_transfer_risk")
    private BigDecimal recentNegativeTransferRisk;

    @TableField("recent_avg_reaction_time_ms")
    private Long recentAvgReactionTimeMs;

    @TableField("last_active_at")
    private LocalDateTime lastActiveAt;

    @TableField("snapshot_json")
    private String snapshotJson;

    @TableField("snapshot_at")
    private LocalDateTime snapshotAt;

    private Integer version;

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Long getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(Long studentUserId) {
        this.studentUserId = studentUserId;
    }

    public Long getTeachingClassId() {
        return teachingClassId;
    }

    public void setTeachingClassId(Long teachingClassId) {
        this.teachingClassId = teachingClassId;
    }

    public Long getTeacherUserId() {
        return teacherUserId;
    }

    public void setTeacherUserId(Long teacherUserId) {
        this.teacherUserId = teacherUserId;
    }

    public Long getLastDiagnosisSummaryId() {
        return lastDiagnosisSummaryId;
    }

    public void setLastDiagnosisSummaryId(Long lastDiagnosisSummaryId) {
        this.lastDiagnosisSummaryId = lastDiagnosisSummaryId;
    }

    public Long getLastTrainingSessionId() {
        return lastTrainingSessionId;
    }

    public void setLastTrainingSessionId(Long lastTrainingSessionId) {
        this.lastTrainingSessionId = lastTrainingSessionId;
    }

    public String getPrimaryRiskLevel() {
        return primaryRiskLevel;
    }

    public void setPrimaryRiskLevel(String primaryRiskLevel) {
        this.primaryRiskLevel = primaryRiskLevel;
    }

    public String getRecommendedTrainingMode() {
        return recommendedTrainingMode;
    }

    public void setRecommendedTrainingMode(String recommendedTrainingMode) {
        this.recommendedTrainingMode = recommendedTrainingMode;
    }

    public Integer getPendingReviewCount() {
        return pendingReviewCount;
    }

    public void setPendingReviewCount(Integer pendingReviewCount) {
        this.pendingReviewCount = pendingReviewCount;
    }

    public Integer getHighRiskPairCount() {
        return highRiskPairCount;
    }

    public void setHighRiskPairCount(Integer highRiskPairCount) {
        this.highRiskPairCount = highRiskPairCount;
    }

    public BigDecimal getRecentAccuracy() {
        return recentAccuracy;
    }

    public void setRecentAccuracy(BigDecimal recentAccuracy) {
        this.recentAccuracy = recentAccuracy;
    }

    public BigDecimal getRecentNegativeTransferRisk() {
        return recentNegativeTransferRisk;
    }

    public void setRecentNegativeTransferRisk(BigDecimal recentNegativeTransferRisk) {
        this.recentNegativeTransferRisk = recentNegativeTransferRisk;
    }

    public Long getRecentAvgReactionTimeMs() {
        return recentAvgReactionTimeMs;
    }

    public void setRecentAvgReactionTimeMs(Long recentAvgReactionTimeMs) {
        this.recentAvgReactionTimeMs = recentAvgReactionTimeMs;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public LocalDateTime getSnapshotAt() {
        return snapshotAt;
    }

    public void setSnapshotAt(LocalDateTime snapshotAt) {
        this.snapshotAt = snapshotAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
