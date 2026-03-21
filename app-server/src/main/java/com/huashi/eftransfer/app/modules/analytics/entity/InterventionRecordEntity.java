package com.huashi.eftransfer.app.modules.analytics.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("intervention_record")
public class InterventionRecordEntity extends BaseAuditEntity {

    @TableField("teacher_user_id")
    private Long teacherUserId;

    @TableField("teaching_class_id")
    private Long teachingClassId;

    @TableField("student_user_id")
    private Long studentUserId;

    @TableField("intervention_type")
    private String interventionType;

    private String status;
    private String priority;

    @TableField("trigger_source")
    private String triggerSource;

    @TableField("trigger_snapshot_json")
    private String triggerSnapshotJson;

    private String note;

    @TableField("planned_at")
    private LocalDateTime plannedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    public Long getTeacherUserId() {
        return teacherUserId;
    }

    public void setTeacherUserId(Long teacherUserId) {
        this.teacherUserId = teacherUserId;
    }

    public Long getTeachingClassId() {
        return teachingClassId;
    }

    public void setTeachingClassId(Long teachingClassId) {
        this.teachingClassId = teachingClassId;
    }

    public Long getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(Long studentUserId) {
        this.studentUserId = studentUserId;
    }

    public String getInterventionType() {
        return interventionType;
    }

    public void setInterventionType(String interventionType) {
        this.interventionType = interventionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public String getTriggerSnapshotJson() {
        return triggerSnapshotJson;
    }

    public void setTriggerSnapshotJson(String triggerSnapshotJson) {
        this.triggerSnapshotJson = triggerSnapshotJson;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getPlannedAt() {
        return plannedAt;
    }

    public void setPlannedAt(LocalDateTime plannedAt) {
        this.plannedAt = plannedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
