package com.huashi.eftransfer.shared.ai;

public record RagDiagnosticSummary(
        Double negativeTransferRisk,
        Double contextSensitivity,
        Double overallAccuracy,
        Long averageReactionTimeMs
) {
}
