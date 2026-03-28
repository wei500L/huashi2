package com.huashi.eftransfer.app.modules.lexicon.vo;

import java.time.LocalDateTime;

public record LexicalListSummaryVO(
        Long id,
        String listName,
        String description,
        Long ownerUserId,
        String ownerDisplayName,
        Boolean active,
        long itemCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
