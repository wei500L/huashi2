package com.huashi.eftransfer.app.modules.training.vo;

import com.huashi.eftransfer.shared.enums.LexicalPairType;

public record TrainingRiskWordVO(
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        LexicalPairType lexicalPairType,
        String reason,
        String riskLevel,
        String dominantErrorType
) {
}
