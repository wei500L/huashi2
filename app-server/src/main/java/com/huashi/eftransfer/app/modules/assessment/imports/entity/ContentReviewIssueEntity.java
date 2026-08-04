package com.huashi.eftransfer.app.modules.assessment.imports.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("assessment_content_review_issue")
public class ContentReviewIssueEntity extends BaseAuditEntity {
    @TableField("import_id") private Long importId;
    @TableField("question_version_id") private Long questionVersionId;
    @TableField("issue_code") private String issueCode;
    private String severity;
    private String status;
    @TableField("source_reference") private String sourceReference;
    private String description;
    @TableField("source_value_json") private String sourceValueJson;
    @TableField("candidate_value_json") private String candidateValueJson;
    @TableField("resolved_by") private Long resolvedBy;
    @TableField("resolved_at") private LocalDateTime resolvedAt;
    @TableField("resolution_note") private String resolutionNote;

    public Long getImportId() { return importId; }
    public void setImportId(Long value) { this.importId = value; }
    public Long getQuestionVersionId() { return questionVersionId; }
    public void setQuestionVersionId(Long value) { this.questionVersionId = value; }
    public String getIssueCode() { return issueCode; }
    public void setIssueCode(String value) { this.issueCode = value; }
    public String getSeverity() { return severity; }
    public void setSeverity(String value) { this.severity = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String value) { this.sourceReference = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { this.description = value; }
    public String getSourceValueJson() { return sourceValueJson; }
    public void setSourceValueJson(String value) { this.sourceValueJson = value; }
    public String getCandidateValueJson() { return candidateValueJson; }
    public void setCandidateValueJson(String value) { this.candidateValueJson = value; }
    public Long getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(Long value) { this.resolvedBy = value; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime value) { this.resolvedAt = value; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String value) { this.resolutionNote = value; }
}
