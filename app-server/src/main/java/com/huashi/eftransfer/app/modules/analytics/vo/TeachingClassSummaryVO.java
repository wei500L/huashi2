package com.huashi.eftransfer.app.modules.analytics.vo;

public record TeachingClassSummaryVO(
        Long classId,
        String classCode,
        String className,
        String gradeName,
        long studentCount
) {
}
