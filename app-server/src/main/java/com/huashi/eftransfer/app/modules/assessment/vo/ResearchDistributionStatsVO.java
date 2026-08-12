package com.huashi.eftransfer.app.modules.assessment.vo;

public record ResearchDistributionStatsVO(
        Double average,
        Long median,
        Long q1,
        Long q3,
        Long p90,
        long sampleCount
) {
}
