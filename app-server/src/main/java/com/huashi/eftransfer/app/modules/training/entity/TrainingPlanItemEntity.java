package com.huashi.eftransfer.app.modules.training.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.math.BigDecimal;

@TableName("training_plan_item")
public class TrainingPlanItemEntity extends BaseAuditEntity {

    @TableField("plan_id")
    private Long planId;

    @TableField("lexical_pair_id")
    private Long lexicalPairId;

    @TableField("recommended_mode")
    private String recommendedMode;

    @TableField("recommended_difficulty")
    private Integer recommendedDifficulty;

    @TableField("risk_level")
    private String riskLevel;

    @TableField("priority_score")
    private BigDecimal priorityScore;

    @TableField("recommended_reason")
    private String recommendedReason;

    @TableField("dominant_error_type")
    private String dominantErrorType;

    @TableField("target_context_support")
    private String targetContextSupport;

    @TableField("expected_exposures")
    private Integer expectedExposures;

    @TableField("sort_order")
    private Integer sortOrder;

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Long getLexicalPairId() {
        return lexicalPairId;
    }

    public void setLexicalPairId(Long lexicalPairId) {
        this.lexicalPairId = lexicalPairId;
    }

    public String getRecommendedMode() {
        return recommendedMode;
    }

    public void setRecommendedMode(String recommendedMode) {
        this.recommendedMode = recommendedMode;
    }

    public Integer getRecommendedDifficulty() {
        return recommendedDifficulty;
    }

    public void setRecommendedDifficulty(Integer recommendedDifficulty) {
        this.recommendedDifficulty = recommendedDifficulty;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public BigDecimal getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(BigDecimal priorityScore) {
        this.priorityScore = priorityScore;
    }

    public String getRecommendedReason() {
        return recommendedReason;
    }

    public void setRecommendedReason(String recommendedReason) {
        this.recommendedReason = recommendedReason;
    }

    public String getDominantErrorType() {
        return dominantErrorType;
    }

    public void setDominantErrorType(String dominantErrorType) {
        this.dominantErrorType = dominantErrorType;
    }

    public String getTargetContextSupport() {
        return targetContextSupport;
    }

    public void setTargetContextSupport(String targetContextSupport) {
        this.targetContextSupport = targetContextSupport;
    }

    public Integer getExpectedExposures() {
        return expectedExposures;
    }

    public void setExpectedExposures(Integer expectedExposures) {
        this.expectedExposures = expectedExposures;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
