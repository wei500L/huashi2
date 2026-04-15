package com.huashi.eftransfer.app.modules.ai.vo;

import java.time.LocalDateTime;

public record LexicalRagConversationSummaryVO(
        String conversationId,
        String title,
        LocalDateTime lastMessageAt
) {
}
