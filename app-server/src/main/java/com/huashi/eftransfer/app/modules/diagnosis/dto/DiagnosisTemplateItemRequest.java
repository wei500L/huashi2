package com.huashi.eftransfer.app.modules.diagnosis.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record DiagnosisTemplateItemRequest(
        @NotNull(message = "lexicalPairId must not be null")
        Long lexicalPairId,
        @NotBlank(message = "taskType must not be blank")
        String taskType,
        @NotBlank(message = "blockCode must not be blank")
        @Size(max = 64, message = "blockCode must be less than or equal to 64 characters")
        String blockCode,
        @NotNull(message = "sortOrder must not be null")
        @Min(value = 1, message = "sortOrder must be greater than 0")
        Integer sortOrder,
        @NotBlank(message = "contextSupportLevel must not be blank")
        String contextSupportLevel,
        @NotNull(message = "expectedSemanticMatch must not be null")
        Boolean expectedSemanticMatch,
        @NotNull(message = "stimulus must not be null")
        @Valid
        DiagnosisTemplateStimulusRequest stimulus,
        @NotEmpty(message = "options must not be empty")
        List<@Valid DiagnosisTemplateOptionRequest> options,
        @NotBlank(message = "correctAnswerKey must not be blank")
        @Size(max = 64, message = "correctAnswerKey must be less than or equal to 64 characters")
        String correctAnswerKey,
        @Valid
        DiagnosisTemplateScoringProfileRequest scoringProfile
) {
}
