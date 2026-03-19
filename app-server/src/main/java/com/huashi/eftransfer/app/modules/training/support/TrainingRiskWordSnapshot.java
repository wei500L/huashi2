package com.huashi.eftransfer.app.modules.training.support;

public record TrainingRiskWordSnapshot(
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        String lexicalPairType,
        String reason,
        String riskLevel,
        String dominantErrorType
) {
}
