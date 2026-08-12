package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AssessmentAttemptResponseRequest(
        @NotNull(message = "questionOrder must not be null")
        Integer questionOrder,
        List<@Size(max = 1000, message = "response must be at most 1000 characters") String> responses,
        @Size(max = 2000, message = "justificationText must be at most 2000 characters")
        String justificationText,
        List<@Size(max = 64, message = "attachment token must be at most 64 characters") String> attachmentTokens
) {

    public AssessmentAttemptResponseRequest(Integer questionOrder, List<String> responses) {
        this(questionOrder, responses, null, null);
    }

    public AssessmentAttemptResponseRequest(Integer questionOrder, List<String> responses, String justificationText) {
        this(questionOrder, responses, justificationText, null);
    }
}
