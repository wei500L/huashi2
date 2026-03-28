package com.huashi.eftransfer.app.modules.lexicon.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReorderLexicalListItemsRequest(
        @NotEmpty(message = "orderedItemIds must not be empty")
        List<@NotNull(message = "itemId must not be null") Long> orderedItemIds
) {
}
