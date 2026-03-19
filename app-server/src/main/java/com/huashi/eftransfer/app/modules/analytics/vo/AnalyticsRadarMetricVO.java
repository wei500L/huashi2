package com.huashi.eftransfer.app.modules.analytics.vo;

public record AnalyticsRadarMetricVO(
        String key,
        String label,
        double value,
        double max
) {
}
