package com.huashi.eftransfer.app.modules.analytics.vo;

public record AnalyticsRiskBucketVO(
        double bucketStart,
        double bucketEnd,
        long studentCount
) {
}
