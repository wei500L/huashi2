package com.huashi.eftransfer.app.modules.assessment.imports.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

@TableName("assessment_question_bank")
public class QuestionBankEntity extends BaseAuditEntity {
    @TableField("bank_code") private String bankCode;
    private String name;
    private String description;
    @TableField("owner_user_id") private Long ownerUserId;
    private String visibility;
    private String status;
    public String getBankCode() { return bankCode; }
    public void setBankCode(String value) { this.bankCode = value; }
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { this.description = value; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long value) { this.ownerUserId = value; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String value) { this.visibility = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
}
