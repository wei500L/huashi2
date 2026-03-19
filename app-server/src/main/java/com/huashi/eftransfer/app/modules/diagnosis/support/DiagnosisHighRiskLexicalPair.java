package com.huashi.eftransfer.app.modules.diagnosis.support;

public record DiagnosisHighRiskLexicalPair(
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        String lexicalPairType,
        double riskScore,
        long errorCount,
        long averageReactionTime,
        String dominantErrorType
) {
}
