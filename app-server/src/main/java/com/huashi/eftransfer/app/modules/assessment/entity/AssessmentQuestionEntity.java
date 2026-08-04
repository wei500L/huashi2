package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.math.BigDecimal;

@TableName("assessment_question")
public class AssessmentQuestionEntity extends BaseAuditEntity {

    @TableField("paper_id")
    private Long paperId;

    @TableField("question_type")
    private String questionType;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("stem_text")
    private String stemText;

    @TableField("prompt_text")
    private String promptText;

    @TableField("options_json")
    private String optionsJson;

    @TableField("correct_answer_json")
    private String correctAnswerJson;

    @TableField("explanation_text")
    private String explanationText;

    private Integer score;

    @TableField("question_version_id")
    private Long questionVersionId;

    @TableField("section_code")
    private String sectionCode;

    @TableField("required_answer")
    private Boolean requiredAnswer;

    private BigDecimal weight;

    @TableField("transfer_category")
    private String transferCategory;

    @TableField("context_level")
    private String contextLevel;

    @TableField("construct_code")
    private String constructCode;

    @TableField("target_word")
    private String targetWord;

    @TableField("option_explanations_json")
    private String optionExplanationsJson;

    @TableField("display_condition_json")
    private String displayConditionJson;

    public Long getPaperId() {
        return paperId;
    }

    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getStemText() {
        return stemText;
    }

    public void setStemText(String stemText) {
        this.stemText = stemText;
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public String getCorrectAnswerJson() {
        return correctAnswerJson;
    }

    public void setCorrectAnswerJson(String correctAnswerJson) {
        this.correctAnswerJson = correctAnswerJson;
    }

    public String getExplanationText() {
        return explanationText;
    }

    public void setExplanationText(String explanationText) {
        this.explanationText = explanationText;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Long getQuestionVersionId() { return questionVersionId; }
    public void setQuestionVersionId(Long questionVersionId) { this.questionVersionId = questionVersionId; }
    public String getSectionCode() { return sectionCode; }
    public void setSectionCode(String sectionCode) { this.sectionCode = sectionCode; }
    public Boolean getRequiredAnswer() { return requiredAnswer; }
    public void setRequiredAnswer(Boolean requiredAnswer) { this.requiredAnswer = requiredAnswer; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public String getTransferCategory() { return transferCategory; }
    public void setTransferCategory(String transferCategory) { this.transferCategory = transferCategory; }
    public String getContextLevel() { return contextLevel; }
    public void setContextLevel(String contextLevel) { this.contextLevel = contextLevel; }
    public String getConstructCode() { return constructCode; }
    public void setConstructCode(String constructCode) { this.constructCode = constructCode; }
    public String getTargetWord() { return targetWord; }
    public void setTargetWord(String targetWord) { this.targetWord = targetWord; }
    public String getOptionExplanationsJson() { return optionExplanationsJson; }
    public void setOptionExplanationsJson(String optionExplanationsJson) { this.optionExplanationsJson = optionExplanationsJson; }
    public String getDisplayConditionJson() { return displayConditionJson; }
    public void setDisplayConditionJson(String displayConditionJson) { this.displayConditionJson = displayConditionJson; }
}
