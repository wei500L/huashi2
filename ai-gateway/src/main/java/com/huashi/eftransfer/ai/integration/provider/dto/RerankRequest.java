package com.huashi.eftransfer.ai.integration.provider.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RerankRequest(
        String model,
        @NotBlank(message = "query must not be blank")
        String query,
        @NotEmpty(message = "documents must not be empty")
        List<String> documents,
        @Min(value = 1, message = "topN must be greater than 0")
        int topN
) {
}
