package com.huashi.eftransfer.app.modules.training.vo;

public record TrainingRiskWordVO(
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
