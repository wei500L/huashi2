package com.huashi.eftransfer.app.modules.user.vo;

import java.time.LocalDateTime;

public record StudentLearningGoalVO(
        Integer dailyTrainingTarget,
        int dailyTrainingCompletedToday,
        int dailyTrainingRemaining,
        Integer weeklyAccuracyTarget,
        double weeklyAccuracyCurrent,
        double weeklyAccuracyDelta,
        boolean configured,
        LocalDateTime updatedAt
) {
}
