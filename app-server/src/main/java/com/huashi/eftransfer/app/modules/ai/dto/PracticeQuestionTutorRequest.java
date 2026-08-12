package com.huashi.eftransfer.app.modules.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PracticeQuestionTutorRequest(
        @NotNull(message = "practiceSessionId must not be null")
        Long practiceSessionId,

        @NotNull(message = "questionOrder must not be null")
        @Min(1)
        @Max(10000)
        Integer questionOrder
) {
}
