package com.huashi.eftransfer.app.modules.diagnosis.support;

import java.util.List;

public record DiagnosisChartPayload(
        List<DiagnosisRadarMetric> radarMetrics,
        List<DiagnosisDistributionItem> errorTypeDistribution,
        List<DiagnosisContextPerformance> contextPerformance,
        List<DiagnosisLexicalTypePerformance> lexicalTypePerformance,
        List<DiagnosisHighRiskLexicalPair> topRiskPairs,
        List<DiagnosisResponseTimelinePoint> responseTimeline
) {
}
