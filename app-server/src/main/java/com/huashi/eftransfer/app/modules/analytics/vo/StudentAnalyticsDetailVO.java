package com.huashi.eftransfer.app.modules.analytics.vo;

import java.util.List;

public record StudentAnalyticsDetailVO(
        StudentAnalyticsOverviewVO overview,
        AnalyticsTrendVO trend7d,
        AnalyticsTrendVO trend30d,
        AnalyticsHeatmapVO transferHeatmap,
        AnalyticsScatterVO scatter,
        List<AnalyticsRiskPairVO> highRiskPairs,
        List<AnalyticsErrorDistributionVO> errorDistribution
) {
}
