package com.huashi.eftransfer.app.modules.analytics.vo;

import com.huashi.eftransfer.app.modules.analytics.support.StudentAnalyticsSnapshotPayload;

import java.util.List;

public record StudentAnalyticsOverviewVO(
        Long studentUserId,
        String studentName,
        String gradeName,
        String englishLevel,
        String frenchLevel,
        String primaryRiskLevel,
        String recommendedTrainingMode,
        List<AnalyticsCardVO> cards,
        List<AnalyticsRadarMetricVO> radar,
        List<AnalyticsContextPerformanceVO> contextPerformance,
        StudentAnalyticsSnapshotPayload latestSnapshot
) {
}
