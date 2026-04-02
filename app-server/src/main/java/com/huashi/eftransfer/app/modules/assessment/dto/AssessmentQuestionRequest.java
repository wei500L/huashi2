package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

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
        @NotEmpty(message = "correctAnswers must not be empty")
        List<@NotBlank(message = "correctAnswers contains blank value") String> correctAnswers,
        @Size(max = 1000, message = "explanationText must be at most 1000 characters")
        String explanationText,
        @NotNull(message = "score must not be null")
        @Min(value = 1, message = "score must be greater than 0")
        Integer score
) {
}
