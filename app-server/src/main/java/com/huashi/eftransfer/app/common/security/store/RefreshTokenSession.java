package com.huashi.eftransfer.app.common.security.store;

import java.time.Instant;
import java.util.Set;

public record RefreshTokenSession(
        String refreshTokenHash,
        Long userId,
        String username,
        String displayName,
        Set<String> roles,
        String accessTokenId,
        Instant accessTokenExpiresAt,
        Instant issuedAt,
        Instant expiresAt,
        String userAgentFingerprint,
        String issuedIpAddress
) {
}
