package com.huashi.eftransfer.app.modules.analytics.vo;

import com.huashi.eftransfer.app.modules.analytics.support.ClassAnalyticsSnapshotPayload;

import java.util.List;

public record ClassAnalyticsOverviewVO(
        Long classId,
        String classCode,
        String className,
        int studentCount,
        int activeStudentCount,
        int highRiskStudentCount,
        String primaryRiskLevel,
        List<AnalyticsCardVO> cards,
        List<AnalyticsRadarMetricVO> radar,
        ClassAnalyticsSnapshotPayload latestSnapshot
) {
}
