package com.huashi.eftransfer.app.modules.diagnosis.vo;

public record DiagnosisSummaryMetricsVO(
        double positiveTransferScore,
        double negativeTransferRisk,
        double contextSensitivity,
        double semanticDiscrimination,
        double overallAccuracy,
        long averageReactionTime
) {
}
