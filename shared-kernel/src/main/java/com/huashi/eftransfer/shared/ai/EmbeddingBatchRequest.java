package com.huashi.eftransfer.shared.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record EmbeddingBatchRequest(
        @NotEmpty(message = "texts must not be empty")
        List<@NotBlank(message = "text item must not be blank") String> texts,
        String model,
        String modality,
        @Positive(message = "dimension must be greater than 0")
        Integer dimension
) {
}
