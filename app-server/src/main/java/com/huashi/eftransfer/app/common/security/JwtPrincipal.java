package com.huashi.eftransfer.app.common.security;

import org.springframework.security.core.GrantedAuthority;

import java.time.Instant;
import java.util.Collection;
import java.util.Set;

public record JwtPrincipal(
        Long userId,
        String username,
        String displayName,
        Set<String> roles,
        String tokenId,
        Instant expiresAt,
        Collection<? extends GrantedAuthority> authorities
) {
}
