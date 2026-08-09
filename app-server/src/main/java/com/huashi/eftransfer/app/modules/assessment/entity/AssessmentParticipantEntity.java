package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("assessment_participant")
public class AssessmentParticipantEntity extends BaseAuditEntity {
    @TableField("publish_id") private Long publishId;
    @TableField("participant_type") private String participantType;
    @TableField("user_id") private Long userId;
    @TableField("participation_code_id") private Long participationCodeId;
    @TableField("attempt_id") private Long attemptId;
    @TableField("browser_fingerprint_digest") private String browserFingerprintDigest;
    @TableField("sensitive_profile_ciphertext") private String sensitiveProfileCiphertext;
    @TableField("sensitive_profile_iv") private String sensitiveProfileIv;
    @TableField("sensitive_profile_key_version") private String sensitiveProfileKeyVersion;
    @TableField("consented_at") private LocalDateTime consentedAt;

    public Long getPublishId() { return publishId; }
    public void setPublishId(Long value) { publishId = value; }
    public String getParticipantType() { return participantType; }
    public void setParticipantType(String value) { participantType = value; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { userId = value; }
    public Long getParticipationCodeId() { return participationCodeId; }
    public void setParticipationCodeId(Long value) { participationCodeId = value; }
    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long value) { attemptId = value; }
    public String getBrowserFingerprintDigest() { return browserFingerprintDigest; }
    public void setBrowserFingerprintDigest(String value) { browserFingerprintDigest = value; }
    public String getSensitiveProfileCiphertext() { return sensitiveProfileCiphertext; }
    public void setSensitiveProfileCiphertext(String value) { sensitiveProfileCiphertext = value; }
    public String getSensitiveProfileIv() { return sensitiveProfileIv; }
    public void setSensitiveProfileIv(String value) { sensitiveProfileIv = value; }
    public String getSensitiveProfileKeyVersion() { return sensitiveProfileKeyVersion; }
    public void setSensitiveProfileKeyVersion(String value) { sensitiveProfileKeyVersion = value; }
    public LocalDateTime getConsentedAt() { return consentedAt; }
    public void setConsentedAt(LocalDateTime value) { consentedAt = value; }
}
