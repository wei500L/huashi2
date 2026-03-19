package com.huashi.eftransfer.app.modules.analytics.vo;

public record AnalyticsContextPerformanceVO(
        String contextSupportLevel,
        double accuracy,
        long avgReactionTimeMs,
        long attemptCount
) {
}
