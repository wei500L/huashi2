package com.huashi.eftransfer.app.modules.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.shared.model.BaseAuditEntity;

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

    @TableField("composite_score")
    private Integer compositeScore;

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

    public Integer getCompositeScore() {
        return compositeScore;
    }

    public void setCompositeScore(Integer compositeScore) {
        this.compositeScore = compositeScore;
    }
}
