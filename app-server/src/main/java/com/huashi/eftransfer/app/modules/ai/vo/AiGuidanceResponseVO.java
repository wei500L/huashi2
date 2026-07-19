package com.huashi.eftransfer.app.modules.ai.vo;

import com.huashi.eftransfer.shared.ai.RagCitation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AiGuidanceResponseVO(
        @NotBlank(message = "requestId must not be blank")
        String requestId,
        @NotBlank(message = "generationSource must not be blank")
        String generationSource,
        @NotBlank(message = "promptVersion must not be blank")
        String promptVersion,
        String model,
        @NotNull(message = "latencyMs must not be null")
        Long latencyMs,
        @NotEmpty(message = "recommendationPath must not be empty")
        List<@Valid AiRecommendationPathItemVO> recommendationPath,
        @NotEmpty(message = "focusLexicalPairs must not be empty")
        List<@Valid AiFocusLexicalPairVO> focusLexicalPairs,
        @NotEmpty(message = "recommendedTrainingModes must not be empty")
        List<@Valid AiRecommendedTrainingModeVO> recommendedTrainingModes,
        @NotBlank(message = "explanation must not be blank")
        String explanation,
        @NotBlank(message = "teacherNote must not be blank")
        String teacherNote,
        @Valid
        DiagnosisInsightVO diagnosisInsight,
        @DecimalMin(value = "0.0", message = "confidence must be >= 0")
        @DecimalMax(value = "1.0", message = "confidence must be <= 1")
        double confidence,
        String fallbackReason,
        boolean grounded,
        List<@NotBlank(message = "citationIds item must not be blank") String> citationIds,
        List<@Valid RagCitation> citations,
        String uncertaintyNote
) {

    public AiGuidanceResponseVO(
            String requestId,
            String generationSource,
            String promptVersion,
            String model,
            Long latencyMs,
            List<AiRecommendationPathItemVO> recommendationPath,
            List<AiFocusLexicalPairVO> focusLexicalPairs,
            List<AiRecommendedTrainingModeVO> recommendedTrainingModes,
            String explanation,
            String teacherNote,
            DiagnosisInsightVO diagnosisInsight,
            double confidence,
            String fallbackReason
    ) {
        this(
                requestId,
                generationSource,
                promptVersion,
                model,
                latencyMs,
                recommendationPath,
                focusLexicalPairs,
                recommendedTrainingModes,
                explanation,
                teacherNote,
                diagnosisInsight,
                confidence,
                fallbackReason,
                false,
                List.of(),
                List.of(),
                null
        );
    }
}
