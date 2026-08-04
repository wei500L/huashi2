package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("assessment_participant_session")
public class AssessmentParticipantSessionEntity extends BaseAuditEntity {
    @TableField("participant_id") private Long participantId;
    @TableField("session_token_digest") private String sessionTokenDigest;
    @TableField("expires_at") private LocalDateTime expiresAt;
    @TableField("last_seen_at") private LocalDateTime lastSeenAt;
    @TableField("revoked_at") private LocalDateTime revokedAt;

    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long value) { participantId = value; }
    public String getSessionTokenDigest() { return sessionTokenDigest; }
    public void setSessionTokenDigest(String value) { sessionTokenDigest = value; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime value) { expiresAt = value; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime value) { lastSeenAt = value; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime value) { revokedAt = value; }
}
