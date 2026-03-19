package com.huashi.eftransfer.app.modules.analytics.vo;

import java.util.Map;

public record AnalyticsHeatmapMetaVO(
        String range,
        String bucket,
        Map<String, String> filters
) {
}
