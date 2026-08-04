package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

@TableName("assessment_participant")
public class AssessmentParticipantEntity extends BaseAuditEntity {
    @TableField("publish_id") private Long publishId;
    @TableField("participant_type") private String participantType;
    @TableField("user_id") private Long userId;
    @TableField("participation_code_id") private Long participationCodeId;
    @TableField("attempt_id") private Long attemptId;
    @TableField("sensitive_profile_ciphertext") private String sensitiveProfileCiphertext;

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
    public String getSensitiveProfileCiphertext() { return sensitiveProfileCiphertext; }
    public void setSensitiveProfileCiphertext(String value) { sensitiveProfileCiphertext = value; }
}
