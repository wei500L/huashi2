package com.huashi.eftransfer.shared.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RagExplainRiskRequest(
        @Valid
        RagDiagnosticSummary diagnosticSummary,
        @Valid
        @Size(max = 64, message = "errorTypeDistribution size must be less than or equal to 64")
        List<@Valid RagErrorTypeStat> errorTypeDistribution,
        @NotEmpty(message = "highRiskLexicalPairs must not be empty")
        @Size(max = 128, message = "highRiskLexicalPairs size must be less than or equal to 128")
        List<@Valid RagRiskLexicalPair> highRiskLexicalPairs
) {
}
