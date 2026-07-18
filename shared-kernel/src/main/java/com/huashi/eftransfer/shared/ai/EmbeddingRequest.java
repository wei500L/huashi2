package com.huashi.eftransfer.shared.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record EmbeddingRequest(
        @NotBlank(message = "text must not be blank")
        @Size(max = 131072, message = "text is too long")
        String text,
        String model,
        String modality,
        @Positive(message = "dimension must be greater than 0")
        Integer dimension
) {
}
