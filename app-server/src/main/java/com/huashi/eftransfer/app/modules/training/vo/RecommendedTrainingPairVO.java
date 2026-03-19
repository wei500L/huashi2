package com.huashi.eftransfer.app.modules.training.vo;

public record RecommendedTrainingPairVO(
        Long planItemId,
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        String lexicalPairType,
        String recommendedMode,
        Integer recommendedDifficulty,
        String riskLevel,
        Double priorityScore,
        String recommendedReason,
        String dominantErrorType,
        Integer expectedExposures,
        String targetContextSupport
) {
}
