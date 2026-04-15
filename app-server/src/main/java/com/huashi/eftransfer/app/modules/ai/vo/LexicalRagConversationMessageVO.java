package com.huashi.eftransfer.app.modules.ai.vo;

import java.time.LocalDateTime;

public record LexicalRagConversationMessageVO(
        Long messageId,
        String role,
        String content,
        LexicalRagAnswerVO assistantPayload,
        String requestId,
        LocalDateTime createdAt
) {
}
