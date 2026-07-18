package com.huashi.eftransfer.app.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LexicalRagQueryRequest(
        @NotBlank(message = "query must not be blank")
        @Size(max = 32768, message = "query is too long")
        String query,
        @Size(max = 64, message = "conversationId size must be less than or equal to 64")
        String conversationId
) {
}
