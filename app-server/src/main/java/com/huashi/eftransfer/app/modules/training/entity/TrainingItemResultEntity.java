package com.huashi.eftransfer.app.modules.training.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("training_item_result")
public class TrainingItemResultEntity extends BaseAuditEntity {

    @TableField("session_id")
    private Long sessionId;

    @TableField("plan_item_id")
    private Long planItemId;

    @TableField("lexical_pair_id")
    private Long lexicalPairId;

    private String mode;

    @TableField("item_type")
    private String itemType;

    @TableField("presentation_order")
    private Integer presentationOrder;

    @TableField("answer_state")
    private String answerState;

    @TableField("cognitive_tag")
    private String cognitiveTag;

    @TableField("stimulus_json")
    private String stimulusJson;

    @TableField("options_json")
    private String optionsJson;

    @TableField("correct_answer_key")
    private String correctAnswerKey;

    @TableField("selected_answer_key")
    private String selectedAnswerKey;

    @TableField("answer_payload_json")
    private String answerPayloadJson;

    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    @TableField("reaction_time_ms")
    private Integer reactionTimeMs;

    @TableField("hesitation_time_ms")
    private Integer hesitationTimeMs;

    @TableField("is_correct")
    private Boolean isCorrect;

    @TableField("detected_error_type")
    private String detectedErrorType;

    @TableField("review_required")
    private Boolean reviewRequired;

    @TableField("adaptation_action")
    private String adaptationAction;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getPlanItemId() {
        return planItemId;
    }

    public void setPlanItemId(Long planItemId) {
        this.planItemId = planItemId;
    }

    public Long getLexicalPairId() {
        return lexicalPairId;
    }

    public void setLexicalPairId(Long lexicalPairId) {
        this.lexicalPairId = lexicalPairId;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public Integer getPresentationOrder() {
        return presentationOrder;
    }

    public void setPresentationOrder(Integer presentationOrder) {
        this.presentationOrder = presentationOrder;
    }

    public String getAnswerState() {
        return answerState;
    }

    public void setAnswerState(String answerState) {
        this.answerState = answerState;
    }

    public String getCognitiveTag() {
        return cognitiveTag;
    }

    public void setCognitiveTag(String cognitiveTag) {
        this.cognitiveTag = cognitiveTag;
    }

    public String getStimulusJson() {
        return stimulusJson;
    }

    public void setStimulusJson(String stimulusJson) {
        this.stimulusJson = stimulusJson;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public String getCorrectAnswerKey() {
        return correctAnswerKey;
    }

    public void setCorrectAnswerKey(String correctAnswerKey) {
        this.correctAnswerKey = correctAnswerKey;
    }

    public String getSelectedAnswerKey() {
        return selectedAnswerKey;
    }

    public void setSelectedAnswerKey(String selectedAnswerKey) {
        this.selectedAnswerKey = selectedAnswerKey;
    }

    public String getAnswerPayloadJson() {
        return answerPayloadJson;
    }

    public void setAnswerPayloadJson(String answerPayloadJson) {
        this.answerPayloadJson = answerPayloadJson;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public Integer getReactionTimeMs() {
        return reactionTimeMs;
    }

    public void setReactionTimeMs(Integer reactionTimeMs) {
        this.reactionTimeMs = reactionTimeMs;
    }

    public Integer getHesitationTimeMs() {
        return hesitationTimeMs;
    }

    public void setHesitationTimeMs(Integer hesitationTimeMs) {
        this.hesitationTimeMs = hesitationTimeMs;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean correct) {
        isCorrect = correct;
    }

    public String getDetectedErrorType() {
        return detectedErrorType;
    }

    public void setDetectedErrorType(String detectedErrorType) {
        this.detectedErrorType = detectedErrorType;
    }

    public Boolean getReviewRequired() {
        return reviewRequired;
    }

    public void setReviewRequired(Boolean reviewRequired) {
        this.reviewRequired = reviewRequired;
    }

    public String getAdaptationAction() {
        return adaptationAction;
    }

    public void setAdaptationAction(String adaptationAction) {
        this.adaptationAction = adaptationAction;
    }
}
