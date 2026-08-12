package com.huashi.eftransfer.app.modules.assessment.vo;

public record ResearchDimensionStatisticVO(
        String dimension,
        long answeredCount,
        long correctCount,
        Double correctRate
) {
}
