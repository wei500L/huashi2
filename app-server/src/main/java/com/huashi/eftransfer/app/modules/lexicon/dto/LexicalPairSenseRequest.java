package com.huashi.eftransfer.app.modules.lexicon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LexicalPairSenseRequest(
        @Positive(message = "sortOrder must be greater than 0")
        Integer sortOrder,
        @Size(max = 4000, message = "englishDefinition must be less than or equal to 4000 characters")
        String englishDefinition,
        @Size(max = 4000, message = "frenchDefinition must be less than or equal to 4000 characters")
        String frenchDefinition,
        @Size(max = 4000, message = "chineseDefinition must be less than or equal to 4000 characters")
        String chineseDefinition,
        @Valid
        @Size(max = 64, message = "examples size must be less than or equal to 64")
        List<LexicalPairExampleRequest> examples
) {
}
