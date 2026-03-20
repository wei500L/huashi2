package com.huashi.eftransfer.shared.ai;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RagExplainRiskRequest(
        @Valid
        RagDiagnosticSummary diagnosticSummary,
        @Valid
        List<@Valid RagErrorTypeStat> errorTypeDistribution,
        @NotEmpty(message = "highRiskLexicalPairs must not be empty")
        List<@Valid RagRiskLexicalPair> highRiskLexicalPairs
) {
}
