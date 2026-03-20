package com.huashi.eftransfer.shared.ai;

public record RagRiskLexicalPair(
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        String lexicalPairType,
        Double riskScore,
        Long errorCount,
        Long averageReactionTime,
        String dominantErrorType,
        String riskLevel
) {
}
