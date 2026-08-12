package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("research_export_job")
public class ResearchExportJobEntity extends BaseAuditEntity {
    @TableField("publish_id") private Long publishId;
    @TableField("job_key") private String jobKey;
    private String status;
    private String format;
    private String scope;
    @TableField("filter_json") private String filterJson;
    @TableField("include_sensitive_fields") private Boolean includeSensitiveFields;
    @TableField("include_attachment_manifest") private Boolean includeAttachmentManifest;
    @TableField("object_key") private String objectKey;
    @TableField("file_name") private String fileName;
    @TableField("mime_type") private String mimeType;
    @TableField("error_message") private String errorMessage;
    @TableField("requested_by") private Long requestedBy;
    @TableField("requested_at") private LocalDateTime requestedAt;
    @TableField("completed_at") private LocalDateTime completedAt;

    public Long getPublishId() { return publishId; }
    public void setPublishId(Long publishId) { this.publishId = publishId; }
    public String getJobKey() { return jobKey; }
    public void setJobKey(String jobKey) { this.jobKey = jobKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getFilterJson() { return filterJson; }
    public void setFilterJson(String filterJson) { this.filterJson = filterJson; }
    public Boolean getIncludeSensitiveFields() { return includeSensitiveFields; }
    public void setIncludeSensitiveFields(Boolean includeSensitiveFields) { this.includeSensitiveFields = includeSensitiveFields; }
    public Boolean getIncludeAttachmentManifest() { return includeAttachmentManifest; }
    public void setIncludeAttachmentManifest(Boolean includeAttachmentManifest) { this.includeAttachmentManifest = includeAttachmentManifest; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Long getRequestedBy() { return requestedBy; }
    public void setRequestedBy(Long requestedBy) { this.requestedBy = requestedBy; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
