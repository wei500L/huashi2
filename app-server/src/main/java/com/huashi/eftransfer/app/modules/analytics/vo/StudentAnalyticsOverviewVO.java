package com.huashi.eftransfer.app.modules.analytics.vo;

import com.huashi.eftransfer.app.modules.achievement.vo.StudentAchievementWallVO;
import com.huashi.eftransfer.app.modules.analytics.support.StudentAnalyticsSnapshotPayload;
import com.huashi.eftransfer.app.modules.user.vo.StudentLearningGoalVO;

import java.util.List;

public record StudentAnalyticsOverviewVO(
        Long studentUserId,
        String studentName,
        String gradeName,
        String frenchLevel,
        String primaryRiskLevel,
        String recommendedTrainingMode,
        List<AnalyticsCardVO> cards,
        List<AnalyticsRadarMetricVO> radar,
        List<AnalyticsContextPerformanceVO> contextPerformance,
        StudentAnalyticsSnapshotPayload latestSnapshot,
        StudentAchievementWallVO achievementWall,
        StudentLearningGoalVO learningGoal
) {
}
