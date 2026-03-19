package com.huashi.eftransfer.app.modules.analytics.support;

import java.time.LocalDateTime;
import java.util.List;

public record StudentAnalyticsSnapshotPayload(
        String studentName,
        String gradeName,
        String englishLevel,
        String frenchLevel,
        Long lastDiagnosisSummaryId,
        Long lastTrainingSessionId,
        String primaryRiskLevel,
        String recommendedTrainingMode,
        int pendingReviewCount,
        int highRiskPairCount,
        double recentAccuracy,
        double recentNegativeTransferRisk,
        long recentAvgReactionTimeMs,
        LocalDateTime lastActiveAt,
        List<StudentRiskPairPayload> topRiskPairs,
        List<ErrorDistributionPayload> errorDistribution,
        List<String> focusTags
) {

    public record StudentRiskPairPayload(
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
