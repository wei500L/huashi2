package com.huashi.eftransfer.app.modules.ai.vo;

import java.time.LocalDateTime;
import java.util.List;

public record LexicalRagConversationDetailVO(
        String conversationId,
        String title,
        String scene,
        LocalDateTime lastMessageAt,
        List<LexicalRagConversationMessageVO> messages
) {
}
