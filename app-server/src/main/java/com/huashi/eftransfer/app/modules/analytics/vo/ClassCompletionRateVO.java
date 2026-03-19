package com.huashi.eftransfer.app.modules.analytics.vo;

import java.util.List;

public record ClassCompletionRateVO(
        double overallRate,
        long studentCount,
        long completedStudentCount,
        AnalyticsTrendVO trend,
        List<ClassCompletionByModeVO> byMode
) {
}
