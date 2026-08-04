package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

public record AssessmentQuestionRequest(
        @NotBlank(message = "questionType must not be blank")
        String questionType,
        @NotBlank(message = "stemText must not be blank")
        @Size(max = 2000, message = "stemText must be at most 2000 characters")
        String stemText,
        @Size(max = 1000, message = "promptText must be at most 1000 characters")
        String promptText,
        @Valid
        List<AssessmentOptionRequest> options,
        List<@NotBlank(message = "correctAnswers contains blank value") String> correctAnswers,
        @Size(max = 1000, message = "explanationText must be at most 1000 characters")
        String explanationText,
        @NotNull(message = "score must not be null")
        @Min(value = 0, message = "score must not be negative")
        Integer score,
        Long questionVersionId,
        @Size(max = 64, message = "sectionCode must be at most 64 characters") String sectionCode,
        Boolean requiredAnswer,
        BigDecimal weight,
        @Size(max = 32) String transferCategory,
        @Size(max = 32) String contextLevel,
        @Size(max = 64) String constructCode,
        @Size(max = 255) String targetWord,
        Map<String, String> optionExplanations,
        Map<String, Object> displayCondition
) {

    public AssessmentQuestionRequest(
            String questionType, String stemText, String promptText, List<AssessmentOptionRequest> options,
            List<String> correctAnswers, String explanationText, Integer score
    ) {
        this(questionType, stemText, promptText, options, correctAnswers, explanationText, score,
                null, null, true, BigDecimal.ONE, null, null, null, null, Map.of(), Map.of());
    }
}
