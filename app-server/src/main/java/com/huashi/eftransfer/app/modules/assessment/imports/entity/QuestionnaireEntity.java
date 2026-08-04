package com.huashi.eftransfer.app.modules.assessment.imports.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

@TableName("assessment_questionnaire")
public class QuestionnaireEntity extends BaseAuditEntity {
    @TableField("questionnaire_code") private String questionnaireCode;
    private String title;
    private String description;
    @TableField("owner_user_id") private Long ownerUserId;
    private String status;
    @TableField("latest_version_no") private Integer latestVersionNo;
    public String getQuestionnaireCode() { return questionnaireCode; }
    public void setQuestionnaireCode(String value) { this.questionnaireCode = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { this.title = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { this.description = value; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long value) { this.ownerUserId = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public Integer getLatestVersionNo() { return latestVersionNo; }
    public void setLatestVersionNo(Integer value) { this.latestVersionNo = value; }
}
