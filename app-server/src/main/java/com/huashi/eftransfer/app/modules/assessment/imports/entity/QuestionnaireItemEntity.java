package com.huashi.eftransfer.app.modules.assessment.imports.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.math.BigDecimal;

@TableName("assessment_questionnaire_item")
public class QuestionnaireItemEntity extends BaseAuditEntity {
    @TableField("questionnaire_version_id") private Long questionnaireVersionId;
    @TableField("section_id") private Long sectionId;
    @TableField("assessment_question_id") private Long assessmentQuestionId;
    @TableField("question_version_id") private Long questionVersionId;
    @TableField("item_code") private String itemCode;
    @TableField("required_answer") private Boolean requiredAnswer;
    private Boolean scored;
    private BigDecimal weight;
    @TableField("transfer_category") private String transferCategory;
    @TableField("context_level") private String contextLevel;
    @TableField("construct_code") private String constructCode;
    @TableField("target_word") private String targetWord;
    @TableField("option_explanations_json") private String optionExplanationsJson;
    @TableField("display_condition_json") private String displayConditionJson;
    @TableField("presentation_json") private String presentationJson;
    public Long getQuestionnaireVersionId() { return questionnaireVersionId; }
    public void setQuestionnaireVersionId(Long value) { this.questionnaireVersionId = value; }
    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long value) { this.sectionId = value; }
    public Long getAssessmentQuestionId() { return assessmentQuestionId; }
    public void setAssessmentQuestionId(Long value) { this.assessmentQuestionId = value; }
    public Long getQuestionVersionId() { return questionVersionId; }
    public void setQuestionVersionId(Long value) { this.questionVersionId = value; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String value) { this.itemCode = value; }
    public Boolean getRequiredAnswer() { return requiredAnswer; }
    public void setRequiredAnswer(Boolean value) { this.requiredAnswer = value; }
    public Boolean getScored() { return scored; }
    public void setScored(Boolean value) { this.scored = value; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal value) { this.weight = value; }
    public String getTransferCategory() { return transferCategory; }
    public void setTransferCategory(String value) { this.transferCategory = value; }
    public String getContextLevel() { return contextLevel; }
    public void setContextLevel(String value) { this.contextLevel = value; }
    public String getConstructCode() { return constructCode; }
    public void setConstructCode(String value) { this.constructCode = value; }
    public String getTargetWord() { return targetWord; }
    public void setTargetWord(String value) { this.targetWord = value; }
    public String getOptionExplanationsJson() { return optionExplanationsJson; }
    public void setOptionExplanationsJson(String value) { this.optionExplanationsJson = value; }
    public String getDisplayConditionJson() { return displayConditionJson; }
    public void setDisplayConditionJson(String value) { this.displayConditionJson = value; }
    public String getPresentationJson() { return presentationJson; }
    public void setPresentationJson(String value) { this.presentationJson = value; }
}
