package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("assessment_timing_event")
public class AssessmentTimingEventEntity extends BaseAuditEntity {
    @TableField("attempt_id") private Long attemptId;
    @TableField("question_id") private Long questionId;
    @TableField("client_event_id") private String clientEventId;
    @TableField("event_type") private String eventType;
    @TableField("effective_delta_ms") private Integer effectiveDeltaMs;
    @TableField("client_occurred_at") private LocalDateTime clientOccurredAt;
    @TableField("first_presented_at") private LocalDateTime firstPresentedAt;
    @TableField("first_answered_at") private LocalDateTime firstAnsweredAt;
    @TableField("modification_count") private Integer modificationCount;

    public Long getAttemptId() { return attemptId; }
    public void setAttemptId(Long attemptId) { this.attemptId = attemptId; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getClientEventId() { return clientEventId; }
    public void setClientEventId(String clientEventId) { this.clientEventId = clientEventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Integer getEffectiveDeltaMs() { return effectiveDeltaMs; }
    public void setEffectiveDeltaMs(Integer effectiveDeltaMs) { this.effectiveDeltaMs = effectiveDeltaMs; }
    public LocalDateTime getClientOccurredAt() { return clientOccurredAt; }
    public void setClientOccurredAt(LocalDateTime clientOccurredAt) { this.clientOccurredAt = clientOccurredAt; }
    public LocalDateTime getFirstPresentedAt() { return firstPresentedAt; }
    public void setFirstPresentedAt(LocalDateTime firstPresentedAt) { this.firstPresentedAt = firstPresentedAt; }
    public LocalDateTime getFirstAnsweredAt() { return firstAnsweredAt; }
    public void setFirstAnsweredAt(LocalDateTime firstAnsweredAt) { this.firstAnsweredAt = firstAnsweredAt; }
    public Integer getModificationCount() { return modificationCount; }
    public void setModificationCount(Integer modificationCount) { this.modificationCount = modificationCount; }
}
