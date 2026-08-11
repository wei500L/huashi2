package com.huashi.eftransfer.app.modules.assessment.imports.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.math.BigDecimal;

@TableName("assessment_question_version")
public class QuestionVersionEntity extends BaseAuditEntity {
    @TableField("question_bank_id") private Long questionBankId;
    @TableField("question_code") private String questionCode;
    @TableField("version_no") private Integer versionNo;
    @TableField("question_type") private String questionType;
    @TableField("stem_text") private String stemText;
    @TableField("prompt_text") private String promptText;
    @TableField("options_json") private String optionsJson;
    @TableField("correct_answer_json") private String correctAnswerJson;
    @TableField("explanation_text") private String explanationText;
    @TableField("option_explanations_json") private String optionExplanationsJson;
    @TableField("required_answer") private Boolean requiredAnswer;
    private BigDecimal weight;
    @TableField("transfer_category") private String transferCategory;
    @TableField("context_level") private String contextLevel;
    @TableField("construct_code") private String constructCode;
    @TableField("target_word") private String targetWord;
    @TableField("display_condition_json") private String displayConditionJson;
    @TableField("source_reference") private String sourceReference;
    @TableField("content_hash") private String contentHash;

    public Long getQuestionBankId() { return questionBankId; }
    public void setQuestionBankId(Long value) { this.questionBankId = value; }
    public String getQuestionCode() { return questionCode; }
    public void setQuestionCode(String value) { this.questionCode = value; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer value) { this.versionNo = value; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String value) { this.questionType = value; }
    public String getStemText() { return stemText; }
    public void setStemText(String value) { this.stemText = value; }
    public String getPromptText() { return promptText; }
    public void setPromptText(String value) { this.promptText = value; }
    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String value) { this.optionsJson = value; }
    public String getCorrectAnswerJson() { return correctAnswerJson; }
    public void setCorrectAnswerJson(String value) { this.correctAnswerJson = value; }
    public String getExplanationText() { return explanationText; }
    public void setExplanationText(String value) { this.explanationText = value; }
    public String getOptionExplanationsJson() { return optionExplanationsJson; }
    public void setOptionExplanationsJson(String value) { this.optionExplanationsJson = value; }
    public Boolean getRequiredAnswer() { return requiredAnswer; }
    public void setRequiredAnswer(Boolean value) { this.requiredAnswer = value; }
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
    public String getDisplayConditionJson() { return displayConditionJson; }
    public void setDisplayConditionJson(String value) { this.displayConditionJson = value; }
    public String getSourceReference() { return sourceReference; }
    public void setSourceReference(String value) { this.sourceReference = value; }
    public String getContentHash() { return contentHash; }
    public void setContentHash(String value) { this.contentHash = value; }
}
