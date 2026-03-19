package com.huashi.eftransfer.app.modules.analytics.vo;

import java.util.List;

public record AnalyticsTrendVO(
        String bucket,
        List<String> xAxis,
        List<AnalyticsSeriesVO> series
) {
}
