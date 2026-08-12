package com.huashi.eftransfer.app.modules.practice.vo;

public record PracticeSectionMetricVO(
        String sectionCode,
        String title,
        Integer totalCount,
        Integer correctCount,
        Double percentage
) {
}
