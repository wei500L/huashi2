package com.huashi.eftransfer.app.modules.analytics.vo;

import java.util.List;

public record AnalyticsScatterVO(
        String x,
        String y,
        List<AnalyticsScatterPointVO> points
) {
}
