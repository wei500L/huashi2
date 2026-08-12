package com.huashi.eftransfer.app.modules.practice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Batch-saved draft answers of an in-progress practice session. Drafts are
 * not graded; grading happens when the session is completed.
 */
public record SavePracticeDraftRequest(
        @NotEmpty(message = "answers must not be empty")
        @Size(max = 300)
        List<@Valid DraftAnswerItem> answers
) {

    public record DraftAnswerItem(
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
