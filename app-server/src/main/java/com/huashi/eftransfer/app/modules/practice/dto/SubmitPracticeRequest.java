package com.huashi.eftransfer.app.modules.practice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Final answers submitted together when the student finishes a practice
 * session. Every submitted question is graded once, whole-paper style.
 */
public record SubmitPracticeRequest(
        @NotEmpty(message = "answers must not be empty")
        @Size(max = 300)
        List<@Valid AnswerItem> answers
) {

    public record AnswerItem(
            @NotNull(message = "questionOrder must not be null")
            @Min(1)
            @Max(10000)
            Integer questionOrder,

            @NotNull(message = "response must not be null")
            @Size(max = 8)
            List<@Size(max = 500) String> response
    ) {
    }
}
