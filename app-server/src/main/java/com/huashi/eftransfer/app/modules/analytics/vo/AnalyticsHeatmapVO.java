package com.huashi.eftransfer.app.modules.analytics.vo;

import java.util.List;

public record AnalyticsHeatmapVO(
        List<String> xAxis,
        List<String> yAxis,
        List<AnalyticsHeatmapCellVO> cells,
        AnalyticsHeatmapMetaVO meta
) {
}
