package com.huashi.eftransfer.app.modules.diagnosis.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("diagnosis_item_result")
public class DiagnosisItemResultEntity extends BaseAuditEntity {

    @TableField("session_id")
    private Long sessionId;

    @TableField("template_item_id")
    private Long templateItemId;

    @TableField("lexical_pair_id")
    private Long lexicalPairId;

    @TableField("task_type")
    private String taskType;

    @TableField("presentation_order")
    private Integer presentationOrder;

    @TableField("answer_state")
    private String answerState;

    @TableField("stimulus_started_at")
    private LocalDateTime stimulusStartedAt;

    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    @TableField("reaction_time_ms")
    private Integer reactionTimeMs;

    @TableField("hesitation_time_ms")
    private Integer hesitationTimeMs;

    @TableField("selected_answer_key")
    private String selectedAnswerKey;

    @TableField("answer_payload_json")
    private String answerPayloadJson;

    @TableField("is_correct")
    private Boolean isCorrect;

    @TableField("detected_error_type")
    private String detectedErrorType;

    @TableField("semantic_consistent")
    private Boolean semanticConsistent;

    @TableField("transfer_risk_score")
    private BigDecimal transferRiskScore;

    @TableField("item_score")
    private BigDecimal itemScore;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getTemplateItemId() {
        return templateItemId;
    }

    public void setTemplateItemId(Long templateItemId) {
        this.templateItemId = templateItemId;
    }

    public Long getLexicalPairId() {
        return lexicalPairId;
    }

    public void setLexicalPairId(Long lexicalPairId) {
        this.lexicalPairId = lexicalPairId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
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

    public LocalDateTime getStimulusStartedAt() {
        return stimulusStartedAt;
    }

    public void setStimulusStartedAt(LocalDateTime stimulusStartedAt) {
        this.stimulusStartedAt = stimulusStartedAt;
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

    public Boolean getSemanticConsistent() {
        return semanticConsistent;
    }

    public void setSemanticConsistent(Boolean semanticConsistent) {
        this.semanticConsistent = semanticConsistent;
    }

    public BigDecimal getTransferRiskScore() {
        return transferRiskScore;
    }

    public void setTransferRiskScore(BigDecimal transferRiskScore) {
        this.transferRiskScore = transferRiskScore;
    }

    public BigDecimal getItemScore() {
        return itemScore;
    }

    public void setItemScore(BigDecimal itemScore) {
        this.itemScore = itemScore;
    }
}
