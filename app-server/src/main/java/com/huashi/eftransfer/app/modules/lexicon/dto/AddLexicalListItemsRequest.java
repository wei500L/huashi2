package com.huashi.eftransfer.app.modules.lexicon.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddLexicalListItemsRequest(
        @NotEmpty(message = "lexicalPairIds must not be empty")
        List<@NotNull(message = "lexicalPairIds contains null value") Long> lexicalPairIds
) {
}
