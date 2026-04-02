package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

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
}
