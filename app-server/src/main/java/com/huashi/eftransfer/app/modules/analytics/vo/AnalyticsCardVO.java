package com.huashi.eftransfer.app.modules.analytics.vo;

public record AnalyticsCardVO(
        String key,
        String label,
        String unit,
        double value
) {
}
