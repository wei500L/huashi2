package com.huashi.eftransfer.app.modules.training.support;

import java.time.LocalDateTime;
import java.util.List;

public record TrainingLearningProfileSnapshot(
        Long lastDiagnosisSummaryId,
        Long lastTrainingSessionId,
        String priorityMode,
        List<String> modeWeaknesses,
        List<TrainingRiskWordSnapshot> riskLexicalPairs,
        int spacedReviewBacklogCount,
        double recentAccuracy,
        long recentAverageReactionTime,
        LocalDateTime updatedAt
) {
}
