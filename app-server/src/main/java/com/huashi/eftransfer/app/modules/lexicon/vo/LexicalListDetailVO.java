package com.huashi.eftransfer.app.modules.lexicon.vo;

import java.time.LocalDateTime;
import java.util.List;

public record LexicalListDetailVO(
        Long id,
        String listName,
        String description,
        Long ownerUserId,
        String ownerDisplayName,
        Boolean active,
        long itemCount,
        LocalDateTime createdAt,
        List<LexicalListItemVO> items
) {
}
