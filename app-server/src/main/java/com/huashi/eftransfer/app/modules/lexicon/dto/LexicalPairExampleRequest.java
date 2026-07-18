package com.huashi.eftransfer.app.modules.lexicon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record LexicalPairExampleRequest(
        @Positive(message = "sortOrder must be greater than 0")
        Integer sortOrder,
        @Size(max = 4000, message = "englishExample must be less than or equal to 4000 characters")
        String englishExample,
        @Size(max = 4000, message = "frenchExample must be less than or equal to 4000 characters")
        String frenchExample,
        @Size(max = 4000, message = "chineseTranslation must be less than or equal to 4000 characters")
        String chineseTranslation,
        @NotBlank(message = "contextSupportLevel must not be blank")
        String contextSupportLevel,
        @Size(max = 255, message = "source must be less than or equal to 255 characters")
        String source
) {
}
