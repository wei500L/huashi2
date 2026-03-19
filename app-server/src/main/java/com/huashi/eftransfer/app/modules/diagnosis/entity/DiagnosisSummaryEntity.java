package com.huashi.eftransfer.app.modules.diagnosis.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.shared.model.BaseAuditEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("diagnosis_summary")
public class DiagnosisSummaryEntity extends BaseAuditEntity {

    @TableField("session_id")
    private Long sessionId;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("template_id")
    private Long templateId;

    @TableField("positive_transfer_score")
    private BigDecimal positiveTransferScore;

    @TableField("negative_transfer_risk")
    private BigDecimal negativeTransferRisk;

    @TableField("context_sensitivity")
    private BigDecimal contextSensitivity;

    @TableField("semantic_discrimination")
    private BigDecimal semanticDiscrimination;

    @TableField("overall_accuracy")
    private BigDecimal overallAccuracy;

    @TableField("average_reaction_time_ms")
    private Long averageReactionTimeMs;

    @TableField("error_type_distribution_json")
    private String errorTypeDistributionJson;

    @TableField("high_risk_lexical_pairs_json")
    private String highRiskLexicalPairsJson;

    @TableField("chart_payload_json")
    private String chartPayloadJson;

    @TableField("generated_at")
    private LocalDateTime generatedAt;

    @TableField("scoring_version")
    private String scoringVersion;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public BigDecimal getPositiveTransferScore() {
        return positiveTransferScore;
    }

    public void setPositiveTransferScore(BigDecimal positiveTransferScore) {
        this.positiveTransferScore = positiveTransferScore;
    }

    public BigDecimal getNegativeTransferRisk() {
        return negativeTransferRisk;
    }

    public void setNegativeTransferRisk(BigDecimal negativeTransferRisk) {
        this.negativeTransferRisk = negativeTransferRisk;
    }

    public BigDecimal getContextSensitivity() {
        return contextSensitivity;
    }

    public void setContextSensitivity(BigDecimal contextSensitivity) {
        this.contextSensitivity = contextSensitivity;
    }

    public BigDecimal getSemanticDiscrimination() {
        return semanticDiscrimination;
    }

    public void setSemanticDiscrimination(BigDecimal semanticDiscrimination) {
        this.semanticDiscrimination = semanticDiscrimination;
    }

    public BigDecimal getOverallAccuracy() {
        return overallAccuracy;
    }

    public void setOverallAccuracy(BigDecimal overallAccuracy) {
        this.overallAccuracy = overallAccuracy;
    }

    public Long getAverageReactionTimeMs() {
        return averageReactionTimeMs;
    }

    public void setAverageReactionTimeMs(Long averageReactionTimeMs) {
        this.averageReactionTimeMs = averageReactionTimeMs;
    }

    public String getErrorTypeDistributionJson() {
        return errorTypeDistributionJson;
    }

    public void setErrorTypeDistributionJson(String errorTypeDistributionJson) {
        this.errorTypeDistributionJson = errorTypeDistributionJson;
    }

    public String getHighRiskLexicalPairsJson() {
        return highRiskLexicalPairsJson;
    }

    public void setHighRiskLexicalPairsJson(String highRiskLexicalPairsJson) {
        this.highRiskLexicalPairsJson = highRiskLexicalPairsJson;
    }

    public String getChartPayloadJson() {
        return chartPayloadJson;
    }

    public void setChartPayloadJson(String chartPayloadJson) {
        this.chartPayloadJson = chartPayloadJson;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getScoringVersion() {
        return scoringVersion;
    }

    public void setScoringVersion(String scoringVersion) {
        this.scoringVersion = scoringVersion;
    }
}
