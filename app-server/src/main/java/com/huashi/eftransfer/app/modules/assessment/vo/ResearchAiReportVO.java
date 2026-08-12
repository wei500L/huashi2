package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record ResearchAiReportVO(
        Long reportId,
        Long publishId,
        ResearchAggregateSnapshotVO snapshot,
        String promptVersion,
        String status,
        String source,
        String modelName,
        Integer sampleCount,
        Integer promptTokens,
        Integer completionTokens,
        ResearchAiReportContentVO report,
        ResearchAiReportContentVO ruleFallback,
        String fallbackReason,
        LocalDateTime requestedAt,
        LocalDateTime completedAt
) {
}
