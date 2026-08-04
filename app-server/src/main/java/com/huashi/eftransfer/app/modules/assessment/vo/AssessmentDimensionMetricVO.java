package com.huashi.eftransfer.app.modules.assessment.vo;

public record AssessmentDimensionMetricVO(
        String code,
        int correctCount,
        int itemCount,
        Double accuracy
) {
}
