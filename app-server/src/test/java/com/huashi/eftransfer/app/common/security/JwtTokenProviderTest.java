package com.huashi.eftransfer.app.common.security;

import com.huashi.eftransfer.app.common.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    @Test
    void shouldGenerateAndParseAccessToken() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("0123456789abcdef0123456789abcdef");
        jwtProperties.setIssuer("ef-transfer-test");
        jwtProperties.setAccessTokenTtl(Duration.ofMinutes(30));
        jwtProperties.setRefreshTokenTtl(Duration.ofDays(7));

        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        jwtTokenProvider.init();

        AccessToken accessToken = jwtTokenProvider.generateAccessToken(
                99L,
                "admin",
                "System Admin",
                Set.of("ADMIN")
        );

        JwtPrincipal principal = jwtTokenProvider.parseAccessToken(accessToken.token());

        assertEquals(99L, principal.userId());
        assertEquals("admin", principal.username());
        assertEquals("System Admin", principal.displayName());
        assertEquals(Set.of("ADMIN"), principal.roles());
        assertEquals(accessToken.tokenId(), principal.tokenId());
        assertTrue(principal.authorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
    }
}
