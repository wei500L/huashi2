package com.huashi.eftransfer.app.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record LexicalRagQueryRequest(
        @NotBlank(message = "query must not be blank")
        String query
) {
}
