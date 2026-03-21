package com.huashi.eftransfer.app.modules.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("analytics_daily_aggregate")
public class AnalyticsDailyAggregateEntity extends BaseAuditEntity {

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("stat_date")
    private LocalDate statDate;

    @TableField("week_start_date")
    private LocalDate weekStartDate;

    @TableField("source_type")
    private String sourceType;

    @TableField("aggregation_level")
    private String aggregationLevel;

    @TableField("lexical_pair_id")
    private Long lexicalPairId;

    @TableField("lexical_pair_type")
    private String lexicalPairType;

    @TableField("training_mode")
    private String trainingMode;

    @TableField("context_support_level")
    private String contextSupportLevel;

    @TableField("attempt_count")
    private Integer attemptCount;

    @TableField("correct_count")
    private Integer correctCount;

    @TableField("incorrect_count")
    private Integer incorrectCount;

    @TableField("total_reaction_time_ms")
    private Long totalReactionTimeMs;

    @TableField("total_hesitation_time_ms")
    private Long totalHesitationTimeMs;

    @TableField("positive_transfer_score_sum")
    private BigDecimal positiveTransferScoreSum;

    @TableField("negative_transfer_risk_sum")
    private BigDecimal negativeTransferRiskSum;

    @TableField("context_sensitivity_sum")
    private BigDecimal contextSensitivitySum;

    @TableField("semantic_discrimination_sum")
    private BigDecimal semanticDiscriminationSum;

    @TableField("high_risk_count")
    private Integer highRiskCount;

    @TableField("pending_review_count")
    private Integer pendingReviewCount;

    @TableField("completion_count")
    private Integer completionCount;

    @TableField("false_friend_confusion_count")
    private Integer falseFriendConfusionCount;

    @TableField("context_ignored_count")
    private Integer contextIgnoredCount;

    @TableField("over_transfer_count")
    private Integer overTransferCount;

    @TableField("under_transfer_count")
    private Integer underTransferCount;

    @TableField("orthographic_interference_count")
    private Integer orthographicInterferenceCount;

    @TableField("semantic_misfire_count")
    private Integer semanticMisfireCount;

    @TableField("last_event_at")
    private LocalDateTime lastEventAt;

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getAggregationLevel() {
        return aggregationLevel;
    }

    public void setAggregationLevel(String aggregationLevel) {
        this.aggregationLevel = aggregationLevel;
    }

    public Long getLexicalPairId() {
        return lexicalPairId;
    }

    public void setLexicalPairId(Long lexicalPairId) {
        this.lexicalPairId = lexicalPairId;
    }

    public String getLexicalPairType() {
        return lexicalPairType;
    }

    public void setLexicalPairType(String lexicalPairType) {
        this.lexicalPairType = lexicalPairType;
    }

    public String getTrainingMode() {
        return trainingMode;
    }

    public void setTrainingMode(String trainingMode) {
        this.trainingMode = trainingMode;
    }

    public String getContextSupportLevel() {
        return contextSupportLevel;
    }

    public void setContextSupportLevel(String contextSupportLevel) {
        this.contextSupportLevel = contextSupportLevel;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Integer getCorrectCount() {
        return correctCount;
    }

    public void setCorrectCount(Integer correctCount) {
        this.correctCount = correctCount;
    }

    public Integer getIncorrectCount() {
        return incorrectCount;
    }

    public void setIncorrectCount(Integer incorrectCount) {
        this.incorrectCount = incorrectCount;
    }

    public Long getTotalReactionTimeMs() {
        return totalReactionTimeMs;
    }

    public void setTotalReactionTimeMs(Long totalReactionTimeMs) {
        this.totalReactionTimeMs = totalReactionTimeMs;
    }

    public Long getTotalHesitationTimeMs() {
        return totalHesitationTimeMs;
    }

    public void setTotalHesitationTimeMs(Long totalHesitationTimeMs) {
        this.totalHesitationTimeMs = totalHesitationTimeMs;
    }

    public BigDecimal getPositiveTransferScoreSum() {
        return positiveTransferScoreSum;
    }

    public void setPositiveTransferScoreSum(BigDecimal positiveTransferScoreSum) {
        this.positiveTransferScoreSum = positiveTransferScoreSum;
    }

    public BigDecimal getNegativeTransferRiskSum() {
        return negativeTransferRiskSum;
    }

    public void setNegativeTransferRiskSum(BigDecimal negativeTransferRiskSum) {
        this.negativeTransferRiskSum = negativeTransferRiskSum;
    }

    public BigDecimal getContextSensitivitySum() {
        return contextSensitivitySum;
    }

    public void setContextSensitivitySum(BigDecimal contextSensitivitySum) {
        this.contextSensitivitySum = contextSensitivitySum;
    }

    public BigDecimal getSemanticDiscriminationSum() {
        return semanticDiscriminationSum;
    }

    public void setSemanticDiscriminationSum(BigDecimal semanticDiscriminationSum) {
        this.semanticDiscriminationSum = semanticDiscriminationSum;
    }

    public Integer getHighRiskCount() {
        return highRiskCount;
    }

    public void setHighRiskCount(Integer highRiskCount) {
        this.highRiskCount = highRiskCount;
    }

    public Integer getPendingReviewCount() {
        return pendingReviewCount;
    }

    public void setPendingReviewCount(Integer pendingReviewCount) {
        this.pendingReviewCount = pendingReviewCount;
    }

    public Integer getCompletionCount() {
        return completionCount;
    }

    public void setCompletionCount(Integer completionCount) {
        this.completionCount = completionCount;
    }

    public Integer getFalseFriendConfusionCount() {
        return falseFriendConfusionCount;
    }

    public void setFalseFriendConfusionCount(Integer falseFriendConfusionCount) {
        this.falseFriendConfusionCount = falseFriendConfusionCount;
    }

    public Integer getContextIgnoredCount() {
        return contextIgnoredCount;
    }

    public void setContextIgnoredCount(Integer contextIgnoredCount) {
        this.contextIgnoredCount = contextIgnoredCount;
    }

    public Integer getOverTransferCount() {
        return overTransferCount;
    }

    public void setOverTransferCount(Integer overTransferCount) {
        this.overTransferCount = overTransferCount;
    }

    public Integer getUnderTransferCount() {
        return underTransferCount;
    }

    public void setUnderTransferCount(Integer underTransferCount) {
        this.underTransferCount = underTransferCount;
    }

    public Integer getOrthographicInterferenceCount() {
        return orthographicInterferenceCount;
    }

    public void setOrthographicInterferenceCount(Integer orthographicInterferenceCount) {
        this.orthographicInterferenceCount = orthographicInterferenceCount;
    }

    public Integer getSemanticMisfireCount() {
        return semanticMisfireCount;
    }

    public void setSemanticMisfireCount(Integer semanticMisfireCount) {
        this.semanticMisfireCount = semanticMisfireCount;
    }

    public LocalDateTime getLastEventAt() {
        return lastEventAt;
    }

    public void setLastEventAt(LocalDateTime lastEventAt) {
        this.lastEventAt = lastEventAt;
    }
}
