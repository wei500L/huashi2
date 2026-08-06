package com.huashi.eftransfer.app.modules.ai.support;

import com.huashi.eftransfer.app.modules.ai.vo.DiagnosisInsightVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Model-facing guidance payload. Nested pair/mode fields may be incomplete;
 * {@code AiInsightService} back-fills server-approved metadata after validation.
 */
public record AiStructuredGuidancePayload(
        @NotEmpty(message = "recommendationPath must not be empty")
        List<@Valid AiRecommendationPathItemPayload> recommendationPath,
        @NotEmpty(message = "focusLexicalPairs must not be empty")
        List<@Valid ModelFocusLexicalPair> focusLexicalPairs,
        @NotEmpty(message = "recommendedTrainingModes must not be empty")
        List<@Valid ModelRecommendedTrainingMode> recommendedTrainingModes,
        @NotBlank(message = "explanation must not be blank")
        String explanation,
        @NotBlank(message = "teacherNote must not be blank")
        String teacherNote,
        @Valid
        DiagnosisInsightVO diagnosisInsight,
        @DecimalMin(value = "0.0", message = "confidence must be >= 0")
        @DecimalMax(value = "1.0", message = "confidence must be <= 1")
        double confidence,
        @NotNull(message = "citationIds must not be null")
        List<@NotBlank(message = "citationIds item must not be blank") String> citationIds,
        String uncertaintyNote
) {

    public record AiRecommendationPathItemPayload(
            @NotBlank(message = "title must not be blank")
            String title,
            @NotBlank(message = "reason must not be blank")
            String reason,
            @NotBlank(message = "priority must not be blank")
            String priority
    ) {
    }

    public record ModelFocusLexicalPair(
            @NotNull(message = "lexicalPairId must not be null")
            Long lexicalPairId,
            String englishWord,
            String frenchWord,
            String chineseGloss,
            String lexicalPairType,
            Double riskScore,
            String dominantErrorType,
            @NotBlank(message = "focusReason must not be blank")
            String focusReason
    ) {
    }

    public record ModelRecommendedTrainingMode(
            @NotBlank(message = "mode must not be blank")
            String mode,
            String label,
            @NotBlank(message = "reason must not be blank")
            String reason
    ) {
    }
}
