package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

@TableName("assessment_public_release")
public class AssessmentPublicReleaseEntity extends BaseAuditEntity {
    @TableField("publish_id") private Long publishId;
    @TableField("release_code") private String releaseCode;
    @TableField("code_count") private Integer codeCount;
    @TableField("session_ttl_hours") private Integer sessionTtlHours;
    @TableField("qr_entry_enabled") private Boolean qrEntryEnabled;
    private String status;

    public Long getPublishId() { return publishId; }
    public void setPublishId(Long publishId) { this.publishId = publishId; }
    public String getReleaseCode() { return releaseCode; }
    public void setReleaseCode(String releaseCode) { this.releaseCode = releaseCode; }
    public Integer getCodeCount() { return codeCount; }
    public void setCodeCount(Integer codeCount) { this.codeCount = codeCount; }
    public Integer getSessionTtlHours() { return sessionTtlHours; }
    public void setSessionTtlHours(Integer sessionTtlHours) { this.sessionTtlHours = sessionTtlHours; }
    public Boolean getQrEntryEnabled() { return qrEntryEnabled; }
    public void setQrEntryEnabled(Boolean qrEntryEnabled) { this.qrEntryEnabled = qrEntryEnabled; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
