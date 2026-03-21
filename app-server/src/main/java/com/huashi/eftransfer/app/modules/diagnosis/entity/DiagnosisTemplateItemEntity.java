package com.huashi.eftransfer.app.modules.diagnosis.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

@TableName("diagnosis_template_item")
public class DiagnosisTemplateItemEntity extends BaseAuditEntity {

    @TableField("template_id")
    private Long templateId;

    @TableField("lexical_pair_id")
    private Long lexicalPairId;

    @TableField("task_type")
    private String taskType;

    @TableField("block_code")
    private String blockCode;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("context_support_level")
    private String contextSupportLevel;

    @TableField("expected_semantic_match")
    private Boolean expectedSemanticMatch;

    @TableField("stimulus_payload_json")
    private String stimulusPayloadJson;

    @TableField("options_payload_json")
    private String optionsPayloadJson;

    @TableField("correct_answer_key")
    private String correctAnswerKey;

    @TableField("scoring_profile_json")
    private String scoringProfileJson;

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
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

    public String getBlockCode() {
        return blockCode;
    }

    public void setBlockCode(String blockCode) {
        this.blockCode = blockCode;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getContextSupportLevel() {
        return contextSupportLevel;
    }

    public void setContextSupportLevel(String contextSupportLevel) {
        this.contextSupportLevel = contextSupportLevel;
    }

    public Boolean getExpectedSemanticMatch() {
        return expectedSemanticMatch;
    }

    public void setExpectedSemanticMatch(Boolean expectedSemanticMatch) {
        this.expectedSemanticMatch = expectedSemanticMatch;
    }

    public String getStimulusPayloadJson() {
        return stimulusPayloadJson;
    }

    public void setStimulusPayloadJson(String stimulusPayloadJson) {
        this.stimulusPayloadJson = stimulusPayloadJson;
    }

    public String getOptionsPayloadJson() {
        return optionsPayloadJson;
    }

    public void setOptionsPayloadJson(String optionsPayloadJson) {
        this.optionsPayloadJson = optionsPayloadJson;
    }

    public String getCorrectAnswerKey() {
        return correctAnswerKey;
    }

    public void setCorrectAnswerKey(String correctAnswerKey) {
        this.correctAnswerKey = correctAnswerKey;
    }

    public String getScoringProfileJson() {
        return scoringProfileJson;
    }

    public void setScoringProfileJson(String scoringProfileJson) {
        this.scoringProfileJson = scoringProfileJson;
    }
}
