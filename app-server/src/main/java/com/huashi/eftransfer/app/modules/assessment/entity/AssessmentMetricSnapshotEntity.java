package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.math.BigDecimal;

@TableName("assessment_metric_snapshot")
public class AssessmentMetricSnapshotEntity extends BaseAuditEntity {
    @TableField("attempt_id") private Long attemptId;
    @TableField("metric_version") private String metricVersion;
    @TableField("scoring_version") private String scoringVersion;
    @TableField("raw_score") private BigDecimal rawScore;
    @TableField("max_score") private BigDecimal maxScore;
    @TableField("percentage_score") private BigDecimal percentageScore;
    @TableField("metrics_json") private String metricsJson;
    @TableField("quality_flags_json") private String qualityFlagsJson;

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
    public String getMetricVersion() { return metricVersion; }
    public void setMetricVersion(String metricVersion) { this.metricVersion = metricVersion; }
    public String getScoringVersion() { return scoringVersion; }
    public void setScoringVersion(String scoringVersion) { this.scoringVersion = scoringVersion; }
    public BigDecimal getRawScore() { return rawScore; }
    public void setRawScore(BigDecimal rawScore) { this.rawScore = rawScore; }
    public BigDecimal getMaxScore() { return maxScore; }
    public void setMaxScore(BigDecimal maxScore) { this.maxScore = maxScore; }
    public BigDecimal getPercentageScore() { return percentageScore; }
    public void setPercentageScore(BigDecimal percentageScore) { this.percentageScore = percentageScore; }
    public String getMetricsJson() { return metricsJson; }
    public void setMetricsJson(String metricsJson) { this.metricsJson = metricsJson; }
    public String getQualityFlagsJson() { return qualityFlagsJson; }
    public void setQualityFlagsJson(String qualityFlagsJson) { this.qualityFlagsJson = qualityFlagsJson; }
}
