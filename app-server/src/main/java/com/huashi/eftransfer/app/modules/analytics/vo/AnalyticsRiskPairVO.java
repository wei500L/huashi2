package com.huashi.eftransfer.app.modules.analytics.vo;

public record AnalyticsRiskPairVO(
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        String lexicalPairType,
        double riskScore,
        int attemptCount,
        int incorrectCount
) {
}
