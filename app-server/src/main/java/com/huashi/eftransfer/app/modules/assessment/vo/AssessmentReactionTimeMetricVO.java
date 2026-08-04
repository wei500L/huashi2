package com.huashi.eftransfer.app.modules.assessment.vo;

public record AssessmentReactionTimeMetricVO(
        Long medianMs,
        Long firstQuartileMs,
        Long thirdQuartileMs,
        int sampleCount
) {
}
