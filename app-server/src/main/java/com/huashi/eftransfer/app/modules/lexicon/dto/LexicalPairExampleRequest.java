package com.huashi.eftransfer.app.modules.lexicon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record LexicalPairExampleRequest(
        @Positive(message = "sortOrder must be greater than 0")
        Integer sortOrder,
        String englishExample,
        String frenchExample,
        String chineseTranslation,
        @NotBlank(message = "contextSupportLevel must not be blank")
        String contextSupportLevel,
        String source
) {
}
