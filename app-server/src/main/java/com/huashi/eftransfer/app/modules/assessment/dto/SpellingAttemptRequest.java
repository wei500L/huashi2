package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A single candidate answer submitted for a SPELLING question.
 *
 * The candidate is graded server-side; the full correct answer is never
 * returned to the client before the attempt is submitted.
 */
public record SpellingAttemptRequest(
        @NotNull(message = "questionOrder must not be null")
        @Min(value = 1, message = "questionOrder must be greater than 0")
        Integer questionOrder,
        @NotBlank(message = "candidate must not be blank")
        @Size(max = 255, message = "candidate must be at most 255 characters")
        String candidate
) {
}
