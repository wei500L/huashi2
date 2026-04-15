package com.huashi.eftransfer.app.modules.ai.vo;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record DiagnosisInsightVO(
        @NotEmpty(message = "strengths must not be empty")
        List<String> strengths,
        @NotEmpty(message = "weaknesses must not be empty")
        List<String> weaknesses,
        @NotEmpty(message = "suggestions must not be empty")
        List<String> suggestions
) {
}
