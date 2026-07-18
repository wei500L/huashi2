package com.huashi.eftransfer.shared.ai;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RerankRequest(
        @Size(max = 128, message = "model is too long")
        String model,
        @NotBlank(message = "query must not be blank")
        @Size(max = 32768, message = "query is too long")
        String query,
        @NotEmpty(message = "documents must not be empty")
        @Size(max = 128, message = "documents size must be less than or equal to 128")
        List<@NotBlank(message = "document item must not be blank") @Size(max = 131072, message = "document item is too long") String> documents,
        @Min(value = 1, message = "topN must be greater than 0")
        Integer topN,
        Boolean returnDocuments,
        @Size(max = 32, message = "modality is too long")
        String modality,
        @Size(max = 8192, message = "instruct is too long")
        String instruct
) {
}
