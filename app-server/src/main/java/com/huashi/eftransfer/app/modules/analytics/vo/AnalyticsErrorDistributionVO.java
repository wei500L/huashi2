package com.huashi.eftransfer.app.modules.analytics.vo;

public record AnalyticsErrorDistributionVO(
        String key,
        String label,
        long count,
        double ratio
) {
}
