package com.huashi.eftransfer.app.modules.analytics.vo;

public record AnalyticsHeatmapCellVO(
        String xKey,
        String yKey,
        long value,
        double accuracy,
        long avgReactionTimeMs
) {
}
