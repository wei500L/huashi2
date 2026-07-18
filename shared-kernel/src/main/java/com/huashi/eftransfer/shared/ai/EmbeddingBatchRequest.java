package com.huashi.eftransfer.shared.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EmbeddingBatchRequest(
        @NotEmpty(message = "texts must not be empty")
        @Size(max = 128, message = "texts size must be less than or equal to 128")
        List<@NotBlank(message = "text item must not be blank") @Size(max = 131072, message = "text item is too long") String> texts,
        String model,
        String modality,
        @Positive(message = "dimension must be greater than 0")
        Integer dimension
) {
}
