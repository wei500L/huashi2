package com.huashi.eftransfer.app.modules.lexicon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record LexicalPairSenseRequest(
        @Positive(message = "sortOrder must be greater than 0")
        Integer sortOrder,
        String englishDefinition,
        String frenchDefinition,
        String chineseDefinition,
        @Valid
        List<LexicalPairExampleRequest> examples
) {
}
