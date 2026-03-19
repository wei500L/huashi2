package com.huashi.eftransfer.app.modules.training.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitTrainingAnswerRequest(
        @NotNull(message = "itemResultId must not be null")
        Long itemResultId,
        @NotBlank(message = "selectedAnswerKey must not be blank")
        @Size(max = 64, message = "selectedAnswerKey must be less than or equal to 64 characters")
        String selectedAnswerKey,
        @NotNull(message = "reactionTimeMs must not be null")
        @Min(value = 1, message = "reactionTimeMs must be greater than 0")
        @Max(value = 100000, message = "reactionTimeMs must be less than or equal to 100000")
        Integer reactionTimeMs,
        @NotNull(message = "hesitationTimeMs must not be null")
        @Min(value = 0, message = "hesitationTimeMs must be greater than or equal to 0")
        @Max(value = 100000, message = "hesitationTimeMs must be less than or equal to 100000")
        Integer hesitationTimeMs
) {
}
