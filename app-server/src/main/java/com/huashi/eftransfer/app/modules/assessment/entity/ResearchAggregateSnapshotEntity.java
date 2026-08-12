package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("research_aggregate_snapshot")
public class ResearchAggregateSnapshotEntity extends BaseAuditEntity {
    @TableField("publish_id") private Long publishId;
    @TableField("paper_id") private Long paperId;
    @TableField("snapshot_key") private String snapshotKey;
    @TableField("snapshot_version") private String snapshotVersion;
    @TableField("filter_json") private String filterJson;
    @TableField("sample_count") private Integer sampleCount;
    @TableField("submitted_count") private Integer submittedCount;
    @TableField("statistics_json") private String statisticsJson;
    @TableField("quality_summary_json") private String qualitySummaryJson;
    @TableField("source_max_updated_at") private LocalDateTime sourceMaxUpdatedAt;

    public Long getPublishId() { return publishId; }
    public void setPublishId(Long publishId) { this.publishId = publishId; }
    public Long getPaperId() { return paperId; }
    public void setPaperId(Long paperId) { this.paperId = paperId; }
    public String getSnapshotKey() { return snapshotKey; }
    public void setSnapshotKey(String snapshotKey) { this.snapshotKey = snapshotKey; }
    public String getSnapshotVersion() { return snapshotVersion; }
    public void setSnapshotVersion(String snapshotVersion) { this.snapshotVersion = snapshotVersion; }
    public String getFilterJson() { return filterJson; }
    public void setFilterJson(String filterJson) { this.filterJson = filterJson; }
    public Integer getSampleCount() { return sampleCount; }
    public void setSampleCount(Integer sampleCount) { this.sampleCount = sampleCount; }
    public Integer getSubmittedCount() { return submittedCount; }
    public void setSubmittedCount(Integer submittedCount) { this.submittedCount = submittedCount; }
    public String getStatisticsJson() { return statisticsJson; }
    public void setStatisticsJson(String statisticsJson) { this.statisticsJson = statisticsJson; }
    public String getQualitySummaryJson() { return qualitySummaryJson; }
    public void setQualitySummaryJson(String qualitySummaryJson) { this.qualitySummaryJson = qualitySummaryJson; }
    public LocalDateTime getSourceMaxUpdatedAt() { return sourceMaxUpdatedAt; }
    public void setSourceMaxUpdatedAt(LocalDateTime sourceMaxUpdatedAt) { this.sourceMaxUpdatedAt = sourceMaxUpdatedAt; }
}
