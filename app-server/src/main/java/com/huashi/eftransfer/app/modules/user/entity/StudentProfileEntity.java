package com.huashi.eftransfer.app.modules.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.shared.model.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("student_profile")
public class StudentProfileEntity extends BaseAuditEntity {

    @TableField("user_id")
    private Long userId;

    @TableField("student_no")
    private String studentNo;

    @TableField("grade_name")
    private String gradeName;

    @TableField("english_level")
    private String englishLevel;

    @TableField("french_level")
    private String frenchLevel;

    @TableField("course_stage")
    private String courseStage;

    @TableField("composite_score")
    private Integer compositeScore;

    @TableField("learning_profile_snapshot_json")
    private String learningProfileSnapshotJson;

    @TableField("learning_profile_updated_at")
    private LocalDateTime learningProfileUpdatedAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public String getGradeName() {
        return gradeName;
    }

    public void setGradeName(String gradeName) {
        this.gradeName = gradeName;
    }

    public String getEnglishLevel() {
        return englishLevel;
    }

    public void setEnglishLevel(String englishLevel) {
        this.englishLevel = englishLevel;
    }

    public String getFrenchLevel() {
        return frenchLevel;
    }

    public void setFrenchLevel(String frenchLevel) {
        this.frenchLevel = frenchLevel;
    }

    public String getCourseStage() {
        return courseStage;
    }

    public void setCourseStage(String courseStage) {
        this.courseStage = courseStage;
    }

    public Integer getCompositeScore() {
        return compositeScore;
    }

    public void setCompositeScore(Integer compositeScore) {
        this.compositeScore = compositeScore;
    }

    public String getLearningProfileSnapshotJson() {
        return learningProfileSnapshotJson;
    }

    public void setLearningProfileSnapshotJson(String learningProfileSnapshotJson) {
        this.learningProfileSnapshotJson = learningProfileSnapshotJson;
    }

    public LocalDateTime getLearningProfileUpdatedAt() {
        return learningProfileUpdatedAt;
    }

    public void setLearningProfileUpdatedAt(LocalDateTime learningProfileUpdatedAt) {
        this.learningProfileUpdatedAt = learningProfileUpdatedAt;
    }
}
