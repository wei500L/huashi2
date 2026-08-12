package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record ResearchStatisticsMetaVO(
        ResearchFilterEchoVO filterEcho,
        long sampleCount,
        LocalDateTime generatedAt,
        String metricVersion
) {
}
