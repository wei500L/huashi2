package com.huashi.eftransfer.app.modules.practice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PracticeSpellingCheckRequest(
        @NotNull(message = "questionOrder must not be null")
        @Min(1)
        @Max(10000)
        Integer questionOrder,

        @NotBlank(message = "candidate must not be blank")
        @Size(max = 500)
        String candidate
) {
}
