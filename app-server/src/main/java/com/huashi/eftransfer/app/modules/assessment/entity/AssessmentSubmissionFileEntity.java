package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("assessment_submission_file")
public class AssessmentSubmissionFileEntity extends BaseAuditEntity {
    @TableField("attempt_id") private Long attemptId;
    @TableField("answer_id") private Long answerId;
    @TableField("question_id") private Long questionId;
    @TableField("participant_id") private Long participantId;
    @TableField("student_user_id") private Long studentUserId;
    @TableField("upload_token") private String uploadToken;
    @TableField("original_file_name") private String originalFileName;
    @TableField("storage_provider") private String storageProvider;
    @TableField("object_key") private String objectKey;
    @TableField("mime_type") private String mimeType;
    @TableField("file_extension") private String fileExtension;
    @TableField("size_bytes") private Long sizeBytes;
    private String sha256;
    @TableField("scan_status") private String scanStatus;
    @TableField("binding_status") private String bindingStatus;
    @TableField("uploaded_at") private LocalDateTime uploadedAt;
    @TableField("bound_at") private LocalDateTime boundAt;
    @TableField("deleted_at") private LocalDateTime deletedAt;

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
    public Long getAnswerId() { return answerId; }
    public void setAnswerId(Long answerId) { this.answerId = answerId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long participantId) { this.participantId = participantId; }
    public Long getStudentUserId() { return studentUserId; }
    public void setStudentUserId(Long studentUserId) { this.studentUserId = studentUserId; }
    public String getUploadToken() { return uploadToken; }
    public void setUploadToken(String uploadToken) { this.uploadToken = uploadToken; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getStorageProvider() { return storageProvider; }
    public void setStorageProvider(String storageProvider) { this.storageProvider = storageProvider; }
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getFileExtension() { return fileExtension; }
    public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public String getScanStatus() { return scanStatus; }
    public void setScanStatus(String scanStatus) { this.scanStatus = scanStatus; }
    public String getBindingStatus() { return bindingStatus; }
    public void setBindingStatus(String bindingStatus) { this.bindingStatus = bindingStatus; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public LocalDateTime getBoundAt() { return boundAt; }
    public void setBoundAt(LocalDateTime boundAt) { this.boundAt = boundAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
