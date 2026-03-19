package com.huashi.eftransfer.app.common.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public record JwtPrincipal(
        String username,
        String role,
        Collection<? extends GrantedAuthority> authorities
) {
}
