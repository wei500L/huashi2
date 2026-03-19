package com.huashi.eftransfer.app.modules.training.vo;

import java.time.LocalDateTime;
import java.util.List;

public record RecommendedTrainingPlanVO(
        Long planId,
        Long sourceDiagnosisSessionId,
        Long sourceDiagnosisSummaryId,
        String status,
        String priorityMode,
        Integer recommendedDifficulty,
        String riskLevel,
        Integer estimatedTrainingVolume,
        String recommendationReason,
        List<String> targetMetrics,
        List<TrainingSuggestedSessionVO> suggestedSessions,
        List<RecommendedTrainingPairVO> recommendedPairs,
        LocalDateTime generatedAt
) {
}
