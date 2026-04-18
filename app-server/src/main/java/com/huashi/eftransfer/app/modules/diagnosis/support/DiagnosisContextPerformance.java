package com.huashi.eftransfer.app.modules.diagnosis.support;

import com.huashi.eftransfer.shared.enums.ContextSupportLevel;

public record DiagnosisContextPerformance(
        ContextSupportLevel level,
        double accuracy,
        long avgReactionTime,
        long totalCount
) {
}
