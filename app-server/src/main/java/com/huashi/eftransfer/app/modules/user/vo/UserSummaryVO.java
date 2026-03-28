package com.huashi.eftransfer.app.modules.user.vo;

import java.time.LocalDateTime;
import java.util.Set;

public record UserSummaryVO(
        Long id,
        String username,
        String email,
        String displayName,
        boolean enabled,
        Set<String> roles,
        LocalDateTime lastLoginAt,
        boolean studentProfileLinked,
        boolean teacherProfileLinked,
        String profileLinkStatus,
        String invitationStatus,
        boolean hasActiveSession
) {
}
