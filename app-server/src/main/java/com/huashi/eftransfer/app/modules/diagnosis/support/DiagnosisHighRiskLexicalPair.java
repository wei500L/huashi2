package com.huashi.eftransfer.app.modules.diagnosis.support;

import com.huashi.eftransfer.shared.enums.LexicalPairType;

public record DiagnosisHighRiskLexicalPair(
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        LexicalPairType lexicalPairType,
        double riskScore,
        long errorCount,
        long averageReactionTime,
        String dominantErrorType
) {
}
