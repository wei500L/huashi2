package com.huashi.eftransfer.app.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huashi.eftransfer.app.common.persistence.BaseAuditEntity;

@TableName("ai_conversation_message")
public class LexicalRagConversationMessageEntity extends BaseAuditEntity {

    @TableField("conversation_session_id")
    private Long conversationSessionId;

    private String role;

    @TableField("content_text")
    private String contentText;

    @TableField("payload_json")
    private String payloadJson;

    @TableField("request_id")
    private String requestId;

    @TableField("generation_source")
    private String generationSource;

    private String model;

    private Boolean grounded;

    @TableField("fallback_reason")
    private String fallbackReason;

    public Long getConversationSessionId() {
        return conversationSessionId;
    }

    public void setConversationSessionId(Long conversationSessionId) {
        this.conversationSessionId = conversationSessionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContentText() {
        return contentText;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getGenerationSource() {
        return generationSource;
    }

    public void setGenerationSource(String generationSource) {
        this.generationSource = generationSource;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Boolean getGrounded() {
        return grounded;
    }

    public void setGrounded(Boolean grounded) {
        this.grounded = grounded;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    public void setFallbackReason(String fallbackReason) {
        this.fallbackReason = fallbackReason;
    }
}
