package com.huashi.eftransfer.app.common.security.store;

import java.time.Instant;

public record RegistrationContextSession(
        String tokenHash,
        String classCode,
        String className,
        String gradeName,
        Instant issuedAt,
        Instant expiresAt,
        String issuedIpAddress,
        String userAgentFingerprint
) {
}
