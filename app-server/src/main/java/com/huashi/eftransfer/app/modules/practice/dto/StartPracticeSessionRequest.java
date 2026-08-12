package com.huashi.eftransfer.app.modules.practice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StartPracticeSessionRequest(
        @NotBlank(message = "bankCode must not be blank")
        @Size(max = 64)
        String bankCode,

        @Size(max = 64)
        String sectionCode,

        @Size(max = 50)
        List<@Size(max = 255) String> targetWords
) {
    public StartPracticeSessionRequest {
        targetWords = targetWords == null ? List.of() : List.copyOf(targetWords);
    }

    public StartPracticeSessionRequest(String bankCode, String sectionCode) {
        this(bankCode, sectionCode, List.of());
    }
}
