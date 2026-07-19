package com.huashi.eftransfer.shared.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EmbeddingBatchRequest(
        @NotEmpty(message = "texts must not be empty")
        @Size(max = 32, message = "texts size must be less than or equal to 32")
        List<@NotBlank(message = "text item must not be blank") @Size(max = 131072, message = "text item is too long") String> texts,
        @Size(max = 128, message = "model is too long")
        String model,
        @Size(max = 32, message = "modality is too long")
        String modality,
        @Positive(message = "dimension must be greater than 0")
        @Max(value = 4096, message = "dimension must be less than or equal to 4096")
        Integer dimension
) {
}
