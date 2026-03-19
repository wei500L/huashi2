package com.huashi.eftransfer.app.common.security;

import java.time.Instant;

public record AccessToken(
        String token,
        String tokenId,
        Instant expiresAt
) {
}
