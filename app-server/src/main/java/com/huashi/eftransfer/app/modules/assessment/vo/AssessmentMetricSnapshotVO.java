package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record AssessmentMetricSnapshotVO(
        String scoringVersion,
        Double percentageScore,
        List<AssessmentDimensionMetricVO> dimensions,
        Double cognateAdvantagePoints,
        Double falseFriendInterferencePoints,
        Double contextRepairPoints,
        AssessmentReactionTimeMetricVO reactionTime,
        SpellingMetricVO spelling
) {
    public AssessmentMetricSnapshotVO(
            String scoringVersion, Double percentageScore,
            List<AssessmentDimensionMetricVO> dimensions,
            Double cognateAdvantagePoints, Double falseFriendInterferencePoints,
            Double contextRepairPoints, AssessmentReactionTimeMetricVO reactionTime
    ) {
        this(scoringVersion, percentageScore, dimensions, cognateAdvantagePoints,
                falseFriendInterferencePoints, contextRepairPoints, reactionTime, null);
    }

    public record SpellingMetricVO(
            int firstTryCorrectCount,
            int hintCorrectCount,
            Long preHintMedianMs,
            Long postHintMedianMs
    ) {
    }
}
