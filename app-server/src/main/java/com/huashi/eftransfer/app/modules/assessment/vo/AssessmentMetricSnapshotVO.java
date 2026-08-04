package com.huashi.eftransfer.app.modules.assessment.vo;

import java.util.List;

public record AssessmentMetricSnapshotVO(
        String scoringVersion,
        Double percentageScore,
        List<AssessmentDimensionMetricVO> dimensions,
        Double cognateAdvantagePoints,
        Double falseFriendInterferencePoints,
        Double contextRepairPoints,
        AssessmentReactionTimeMetricVO reactionTime
) {
}
