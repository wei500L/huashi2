package com.huashi.eftransfer.app.modules.analytics.vo;

import java.util.List;

public record AnalyticsSeriesVO(
        String key,
        String label,
        List<Double> values
) {
}
