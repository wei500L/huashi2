package com.huashi.eftransfer.app.modules.training.service;

import com.huashi.eftransfer.shared.enums.RiskLevel;
import com.huashi.eftransfer.shared.enums.TrainingMode;

import java.util.List;

public interface TrainingRecommendationEngine {

    TrainingRecommendation recommend(RecommendationContext context);

    record RecommendationContext(
            Long ownerUserId,
            Long diagnosisSessionId,
            Long diagnosisSummaryId,
            double negativeTransferRisk,
            double contextSensitivity,
            double overallAccuracy,
            long averageReactionTime,
            List<PairSignal> pairSignals
    ) {
    }

    record PairSignal(
            Long lexicalPairId,
            String englishWord,
            String frenchWord,
            String chineseGloss,
            String lexicalPairType,
            double semanticOverlapScore,
            double falseFriendRisk,
            String defaultContextSupport,
            int difficultyLevel,
            double diagnosisRiskScore,
            long errorCount,
            long correctCount,
            long slowCorrectCount,
            long totalExposures,
            long repeatWrongCount,
            long averageReactionTime,
            String dominantErrorType,
            long falseFriendErrorCount,
            long orthographicErrorCount,
            long contextIgnoredCount,
            long underTransferCount,
            boolean hasContextExample
    ) {
    }

    record PairRecommendation(
            Long lexicalPairId,
            TrainingMode recommendedMode,
            int recommendedDifficulty,
            RiskLevel riskLevel,
            double priorityScore,
            String recommendedReason,
            String dominantErrorType,
            String targetContextSupport,
            int expectedExposures
    ) {
    }

    record TrainingRecommendation(
            TrainingMode priorityMode,
            String recommendationReason,
            int recommendedDifficulty,
            RiskLevel riskLevel,
            int estimatedTrainingVolume,
            List<String> targetMetrics,
            List<PairRecommendation> pairRecommendations
    ) {
    }
}
