package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("assessment_participant_access")
public class AssessmentParticipantAccessEntity extends BaseAuditEntity {
    @TableField("public_release_id") private Long publicReleaseId;
    @TableField("participant_id") private Long participantId;
    @TableField("participation_code_id") private Long participationCodeId;
    @TableField("access_mode") private String accessMode;
    @TableField("ip_ciphertext") private String ipCiphertext;
    @TableField("ip_iv") private String ipIv;
    @TableField("ip_key_version") private String ipKeyVersion;
    @TableField("accessed_at") private LocalDateTime accessedAt;

    public Long getPublicReleaseId() { return publicReleaseId; }
    public void setPublicReleaseId(Long value) { publicReleaseId = value; }
    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long value) { participantId = value; }
    public Long getParticipationCodeId() { return participationCodeId; }
    public void setParticipationCodeId(Long value) { participationCodeId = value; }
    public String getAccessMode() { return accessMode; }
    public void setAccessMode(String value) { accessMode = value; }
    public String getIpCiphertext() { return ipCiphertext; }
    public void setIpCiphertext(String value) { ipCiphertext = value; }
    public String getIpIv() { return ipIv; }
    public void setIpIv(String value) { ipIv = value; }
    public String getIpKeyVersion() { return ipKeyVersion; }
    public void setIpKeyVersion(String value) { ipKeyVersion = value; }
    public LocalDateTime getAccessedAt() { return accessedAt; }
    public void setAccessedAt(LocalDateTime value) { accessedAt = value; }
}
