package com.huashi.eftransfer.app.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

import java.time.LocalDateTime;

@TableName("ai_generation_record")
public class AiGenerationRecordEntity extends BaseAuditEntity {

    @TableField("request_id")
    private String requestId;

    private String scene;

    @TableField("student_user_id")
    private Long studentUserId;

    @TableField("teacher_user_id")
    private Long teacherUserId;

    @TableField("teaching_class_id")
    private Long teachingClassId;

    @TableField("diagnosis_summary_id")
    private Long diagnosisSummaryId;

    @TableField("training_session_id")
    private Long trainingSessionId;

    @TableField("intervention_record_id")
    private Long interventionRecordId;

    @TableField("prompt_version")
    private String promptVersion;

    private String model;

    @TableField("provider_request_id")
    private String providerRequestId;

    @TableField("latency_ms")
    private Long latencyMs;

    @TableField("token_usage_json")
    private String tokenUsageJson;

    @TableField("input_payload_json")
    private String inputPayloadJson;

    @TableField("raw_response_json")
    private String rawResponseJson;

    @TableField("validated_output_json")
    private String validatedOutputJson;

    @TableField("generation_source")
    private String generationSource;

    @TableField("fallback_reason")
    private String fallbackReason;

    @TableField("generated_at")
    private LocalDateTime generatedAt;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getScene() {
        return scene;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public Long getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(Long studentUserId) {
        this.studentUserId = studentUserId;
    }

    public Long getTeacherUserId() {
        return teacherUserId;
    }

    public void setTeacherUserId(Long teacherUserId) {
        this.teacherUserId = teacherUserId;
    }

    public Long getTeachingClassId() {
        return teachingClassId;
    }

    public void setTeachingClassId(Long teachingClassId) {
        this.teachingClassId = teachingClassId;
    }

    public Long getDiagnosisSummaryId() {
        return diagnosisSummaryId;
    }

    public void setDiagnosisSummaryId(Long diagnosisSummaryId) {
        this.diagnosisSummaryId = diagnosisSummaryId;
    }

    public Long getTrainingSessionId() {
        return trainingSessionId;
    }

    public void setTrainingSessionId(Long trainingSessionId) {
        this.trainingSessionId = trainingSessionId;
    }

    public Long getInterventionRecordId() {
        return interventionRecordId;
    }

    public void setInterventionRecordId(Long interventionRecordId) {
        this.interventionRecordId = interventionRecordId;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProviderRequestId() {
        return providerRequestId;
    }

    public void setProviderRequestId(String providerRequestId) {
        this.providerRequestId = providerRequestId;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getTokenUsageJson() {
        return tokenUsageJson;
    }

    public void setTokenUsageJson(String tokenUsageJson) {
        this.tokenUsageJson = tokenUsageJson;
    }

    public String getInputPayloadJson() {
        return inputPayloadJson;
    }

    public void setInputPayloadJson(String inputPayloadJson) {
        this.inputPayloadJson = inputPayloadJson;
    }

    public String getRawResponseJson() {
        return rawResponseJson;
    }

    public void setRawResponseJson(String rawResponseJson) {
        this.rawResponseJson = rawResponseJson;
    }

    public String getValidatedOutputJson() {
        return validatedOutputJson;
    }

    public void setValidatedOutputJson(String validatedOutputJson) {
        this.validatedOutputJson = validatedOutputJson;
    }

    public String getGenerationSource() {
        return generationSource;
    }

    public void setGenerationSource(String generationSource) {
        this.generationSource = generationSource;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    public void setFallbackReason(String fallbackReason) {
        this.fallbackReason = fallbackReason;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
