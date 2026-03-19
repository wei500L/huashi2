package com.huashi.eftransfer.app.modules.analytics.vo;

public record AnalyticsScatterPointVO(
        Long lexicalPairId,
        String label,
        String lexicalPairType,
        double accuracy,
        long avgReactionTimeMs,
        int attemptCount,
        double riskScore
) {
}
