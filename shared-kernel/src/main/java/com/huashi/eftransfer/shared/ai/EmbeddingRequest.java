package com.huashi.eftransfer.shared.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record EmbeddingRequest(
        @NotBlank(message = "text must not be blank")
        String text,
        String model,
        String modality,
        @Positive(message = "dimension must be greater than 0")
        Integer dimension
) {
}
