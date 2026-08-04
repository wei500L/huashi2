package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("assessment_participation_code")
public class AssessmentParticipationCodeEntity extends BaseAuditEntity {
    @TableField("public_release_id") private Long publicReleaseId;
    @TableField("code_digest") private String codeDigest;
    private String status;
    @TableField("export_batch_id") private String exportBatchId;
    @TableField("exported_at") private LocalDateTime exportedAt;
    @TableField("first_verified_at") private LocalDateTime firstVerifiedAt;
    @TableField("last_verified_at") private LocalDateTime lastVerifiedAt;
    @TableField("submitted_at") private LocalDateTime submittedAt;

    public Long getPublicReleaseId() { return publicReleaseId; }
    public void setPublicReleaseId(Long value) { publicReleaseId = value; }
    public String getCodeDigest() { return codeDigest; }
    public void setCodeDigest(String value) { codeDigest = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getExportBatchId() { return exportBatchId; }
    public void setExportBatchId(String value) { exportBatchId = value; }
    public LocalDateTime getExportedAt() { return exportedAt; }
    public void setExportedAt(LocalDateTime value) { exportedAt = value; }
    public LocalDateTime getFirstVerifiedAt() { return firstVerifiedAt; }
    public void setFirstVerifiedAt(LocalDateTime value) { firstVerifiedAt = value; }
    public LocalDateTime getLastVerifiedAt() { return lastVerifiedAt; }
    public void setLastVerifiedAt(LocalDateTime value) { lastVerifiedAt = value; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime value) { submittedAt = value; }
}
