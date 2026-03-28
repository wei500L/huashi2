package com.huashi.eftransfer.app.modules.user.vo;

import java.time.Instant;
import java.time.LocalDateTime;

public record SessionOverviewVO(
        LocalDateTime lastLoginAt,
        Instant refreshSessionIssuedAt,
        Instant refreshSessionExpiresAt,
        Instant accessTokenExpiresAt,
        String userAgentFingerprint,
        String issuedIpAddress,
        boolean hasActiveSession
) {
}
