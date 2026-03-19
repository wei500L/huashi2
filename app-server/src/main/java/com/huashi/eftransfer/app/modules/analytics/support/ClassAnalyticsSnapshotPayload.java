package com.huashi.eftransfer.app.modules.analytics.support;

import java.time.LocalDateTime;
import java.util.List;

public record ClassAnalyticsSnapshotPayload(
        String classCode,
        String className,
        String gradeName,
        int studentCount,
        int activeStudentCount,
        int highRiskStudentCount,
        double recentAccuracy,
        double recentNegativeTransferRisk,
        long recentAvgReactionTimeMs,
        String primaryRiskLevel,
        LocalDateTime lastActiveAt,
        List<RiskBucketPayload> riskDistribution,
        List<ModeFocusPayload> recommendedFocusModes,
        List<ClassRiskPairPayload> topRiskPairs,
        List<ErrorDistributionPayload> errorDistribution
) {

    public record RiskBucketPayload(
            String riskLevel,
            long studentCount
    ) {
    }

    public record ModeFocusPayload(
            String mode,
            long studentCount
    ) {
    }

    public record ClassRiskPairPayload(
            Long lexicalPairId,
            String englishWord,
            String frenchWord,
            String lexicalPairType,
            double riskScore,
            int attemptCount,
            int incorrectCount
    ) {
    }

    public record ErrorDistributionPayload(
            String errorType,
            long count,
            double ratio
    ) {
    }
}
