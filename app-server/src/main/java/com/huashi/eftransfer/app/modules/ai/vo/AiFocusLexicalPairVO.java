package com.huashi.eftransfer.app.modules.ai.vo;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiFocusLexicalPairVO(
        @NotNull(message = "lexicalPairId must not be null")
        Long lexicalPairId,
        @NotBlank(message = "englishWord must not be blank")
        String englishWord,
        @NotBlank(message = "frenchWord must not be blank")
        String frenchWord,
        @NotBlank(message = "chineseGloss must not be blank")
        String chineseGloss,
        @NotBlank(message = "lexicalPairType must not be blank")
        String lexicalPairType,
        @DecimalMin(value = "0.0", message = "riskScore must be >= 0")
        @DecimalMax(value = "1.0", message = "riskScore must be <= 1")
        double riskScore,
        @NotBlank(message = "dominantErrorType must not be blank")
        String dominantErrorType,
        @NotBlank(message = "focusReason must not be blank")
        String focusReason
) {
}
