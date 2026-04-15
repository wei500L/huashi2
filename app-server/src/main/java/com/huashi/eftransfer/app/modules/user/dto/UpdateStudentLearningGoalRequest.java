package com.huashi.eftransfer.app.modules.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateStudentLearningGoalRequest(
        @Min(value = 1, message = "Daily training target must be at least 1")
        @Max(value = 500, message = "Daily training target cannot exceed 500")
        Integer dailyTrainingTarget,
        @Min(value = 1, message = "Weekly accuracy target must be at least 1")
        @Max(value = 100, message = "Weekly accuracy target cannot exceed 100")
        Integer weeklyAccuracyTarget
) {
}
