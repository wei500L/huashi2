package com.huashi.eftransfer.app.modules.lexicon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record LexicalPairUpsertRequest(
        @NotBlank(message = "englishWord must not be blank")
        @Size(max = 128, message = "englishWord must be less than or equal to 128 characters")
        String englishWord,
        @NotBlank(message = "frenchWord must not be blank")
        @Size(max = 128, message = "frenchWord must be less than or equal to 128 characters")
        String frenchWord,
        @NotBlank(message = "chineseGloss must not be blank")
        @Size(max = 255, message = "chineseGloss must be less than or equal to 255 characters")
        String chineseGloss,
        @NotBlank(message = "lexicalPairType must not be blank")
        String lexicalPairType,
        @NotNull(message = "semanticOverlapScore must not be null")
        @DecimalMin(value = "0.0", inclusive = true, message = "semanticOverlapScore must be between 0 and 1")
        @DecimalMax(value = "1.0", inclusive = true, message = "semanticOverlapScore must be between 0 and 1")
        BigDecimal semanticOverlapScore,
        @NotNull(message = "falseFriendRisk must not be null")
        @DecimalMin(value = "0.0", inclusive = true, message = "falseFriendRisk must be between 0 and 1")
        @DecimalMax(value = "1.0", inclusive = true, message = "falseFriendRisk must be between 0 and 1")
        BigDecimal falseFriendRisk,
        @NotBlank(message = "defaultContextSupport must not be blank")
        String defaultContextSupport,
        @NotNull(message = "difficultyLevel must not be null")
        @Min(value = 1, message = "difficultyLevel must be between 1 and 5")
        @Max(value = 5, message = "difficultyLevel must be between 1 and 5")
        Integer difficultyLevel,
        @Size(max = 4000, message = "notes must be less than or equal to 4000 characters")
        String notes,
        @Size(max = 255, message = "source must be less than or equal to 255 characters")
        String source,
        @Size(max = 64, message = "sourceCode must be less than or equal to 64 characters")
        String sourceCode,
        @Size(max = 64, message = "contentVersion must be less than or equal to 64 characters")
        String contentVersion,
        @Size(max = 128, message = "wordId must be less than or equal to 128 characters")
        String wordId,
        Boolean active,
        String knowledgeStatus,
        String embeddingStatus,
        @Size(max = 32, message = "tags size must be less than or equal to 32")
        List<@Size(max = 64, message = "tag must be less than or equal to 64 characters") String> tags,
        @Valid
        @Size(max = 32, message = "senses size must be less than or equal to 32")
        List<LexicalPairSenseRequest> senses
) {
}
