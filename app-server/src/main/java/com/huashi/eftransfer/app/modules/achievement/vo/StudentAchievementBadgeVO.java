package com.huashi.eftransfer.app.modules.achievement.vo;

import java.time.LocalDateTime;

public record StudentAchievementBadgeVO(
        String code,
        boolean unlocked,
        int progressValue,
        int targetValue,
        LocalDateTime awardedAt
) {
}
