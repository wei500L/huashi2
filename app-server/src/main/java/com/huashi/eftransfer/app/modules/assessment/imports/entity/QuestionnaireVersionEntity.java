package com.huashi.eftransfer.app.modules.assessment.imports.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

@TableName("assessment_questionnaire_version")
public class QuestionnaireVersionEntity extends BaseAuditEntity {
    @TableField("questionnaire_id") private Long questionnaireId;
    @TableField("paper_id") private Long paperId;
    @TableField("version_no") private Integer versionNo;
    private String status;
    @TableField("scoring_version") private String scoringVersion;
    @TableField("ai_prompt_version") private String aiPromptVersion;
    @TableField("source_package_code") private String sourcePackageCode;
    public Long getQuestionnaireId() { return questionnaireId; }
    public void setQuestionnaireId(Long value) { this.questionnaireId = value; }
    public Long getPaperId() { return paperId; }
    public void setPaperId(Long value) { this.paperId = value; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer value) { this.versionNo = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public String getScoringVersion() { return scoringVersion; }
    public void setScoringVersion(String value) { this.scoringVersion = value; }
    public String getAiPromptVersion() { return aiPromptVersion; }
    public void setAiPromptVersion(String value) { this.aiPromptVersion = value; }
    public String getSourcePackageCode() { return sourcePackageCode; }
    public void setSourcePackageCode(String value) { this.sourcePackageCode = value; }
}
