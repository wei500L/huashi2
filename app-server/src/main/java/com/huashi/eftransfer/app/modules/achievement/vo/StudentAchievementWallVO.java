package com.huashi.eftransfer.app.modules.achievement.vo;

import java.util.List;

public record StudentAchievementWallVO(
        int unlockedCount,
        int totalCount,
        List<StudentAchievementBadgeVO> badges
) {
}
