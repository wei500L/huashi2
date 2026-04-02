package com.huashi.eftransfer.app.modules.assessment.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

@TableName("assessment_attempt_answer")
public class AssessmentAttemptAnswerEntity extends BaseAuditEntity {

    @TableField("attempt_id")
    private Long attemptId;

    @TableField("question_id")
    private Long questionId;

    @TableField("question_order")
    private Integer questionOrder;

    @TableField("question_type")
    private String questionType;

    @TableField("stem_text_snapshot")
    private String stemTextSnapshot;

    @TableField("prompt_text_snapshot")
    private String promptTextSnapshot;

    @TableField("options_json_snapshot")
    private String optionsJsonSnapshot;

    @TableField("correct_answer_json")
    private String correctAnswerJson;

    @TableField("explanation_text_snapshot")
    private String explanationTextSnapshot;

    @TableField("question_score")
    private Integer questionScore;

    @TableField("response_json")
    private String responseJson;

    private Boolean answered;
    private Boolean correct;

    @TableField("score_awarded")
    private Integer scoreAwarded;

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public Integer getQuestionOrder() {
        return questionOrder;
    }

    public void setQuestionOrder(Integer questionOrder) {
        this.questionOrder = questionOrder;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getStemTextSnapshot() {
        return stemTextSnapshot;
    }

    public void setStemTextSnapshot(String stemTextSnapshot) {
        this.stemTextSnapshot = stemTextSnapshot;
    }

    public String getPromptTextSnapshot() {
        return promptTextSnapshot;
    }

    public void setPromptTextSnapshot(String promptTextSnapshot) {
        this.promptTextSnapshot = promptTextSnapshot;
    }

    public String getOptionsJsonSnapshot() {
        return optionsJsonSnapshot;
    }

    public void setOptionsJsonSnapshot(String optionsJsonSnapshot) {
        this.optionsJsonSnapshot = optionsJsonSnapshot;
    }

    public String getCorrectAnswerJson() {
        return correctAnswerJson;
    }

    public void setCorrectAnswerJson(String correctAnswerJson) {
        this.correctAnswerJson = correctAnswerJson;
    }

    public String getExplanationTextSnapshot() {
        return explanationTextSnapshot;
    }

    public void setExplanationTextSnapshot(String explanationTextSnapshot) {
        this.explanationTextSnapshot = explanationTextSnapshot;
    }

    public Integer getQuestionScore() {
        return questionScore;
    }

    public void setQuestionScore(Integer questionScore) {
        this.questionScore = questionScore;
    }

    public String getResponseJson() {
        return responseJson;
    }

    public void setResponseJson(String responseJson) {
        this.responseJson = responseJson;
    }

    public Boolean getAnswered() {
        return answered;
    }

    public void setAnswered(Boolean answered) {
        this.answered = answered;
    }

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
        this.correct = correct;
    }

    public Integer getScoreAwarded() {
        return scoreAwarded;
    }

    public void setScoreAwarded(Integer scoreAwarded) {
        this.scoreAwarded = scoreAwarded;
    }
}
