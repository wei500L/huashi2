package com.huashi.eftransfer.app.common.security;

import com.huashi.eftransfer.app.common.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String CURRENT_SECRET = "x7Pq2Lk9Vd4Nc8Rs1Tf6Yh3Jm5Bw0QeZ";
    private static final String PREVIOUS_SECRET = "m4Cs8Wy1Qp6Jh2Vr9Tk3Nz7Lb5Dx0FuG";
    private static final String LEGACY_SECRET = "r6Mv2Qx8Hs4Pw9Ld1Tg7Kb3Ny5Fc0JeZ";

    @Test
    void shouldGenerateAndParseAccessToken() {
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(createJwtProperties());
        jwtTokenProvider.init();

        AccessToken accessToken = jwtTokenProvider.generateAccessToken(
                99L,
                "admin",
                "System Admin",
                Set.of("ADMIN")
        );

        JwtPrincipal principal = jwtTokenProvider.parseAccessToken(accessToken.token());
        String keyId = OBJECT_MAPPER.readTree(Base64.getUrlDecoder().decode(accessToken.token().split("\\.")[0]))
                .path("kid")
                .asText();

        assertEquals(99L, principal.userId());
        assertEquals("admin", principal.username());
        assertEquals("System Admin", principal.displayName());
        assertEquals(Set.of("ADMIN"), principal.roles());
        assertEquals(accessToken.tokenId(), principal.tokenId());
        assertEquals("current", keyId);
        assertTrue(principal.authorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
    }

    @Test
    void shouldParseLegacyTokenWithoutKidWhenLegacySecretConfigured() {
        JwtProperties jwtProperties = createJwtProperties();
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        jwtTokenProvider.init();

        Instant now = Instant.now();
        String legacyToken = Jwts.builder()
                .subject("legacy-admin")
                .issuer(jwtProperties.getIssuer())
                .id("legacy-token-id")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(10))))
                .claim("uid", 7L)
                .claim("displayName", "Legacy Admin")
                .claim("roles", List.of("ADMIN"))
                .claim("tokenType", "ACCESS")
                .signWith(Keys.hmacShaKeyFor(LEGACY_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        JwtPrincipal principal = jwtTokenProvider.parseAccessToken(legacyToken);

        assertEquals(7L, principal.userId());
        assertEquals("legacy-admin", principal.username());
        assertEquals("legacy-token-id", principal.tokenId());
        assertEquals(Set.of("ADMIN"), principal.roles());
    }

    @Test
    void shouldRejectTokenWithUnknownKid() {
        JwtProperties jwtProperties = createJwtProperties();
        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties);
        jwtTokenProvider.init();

        Instant now = Instant.now();
        String invalidToken = Jwts.builder()
                .header()
                .add("kid", "missing")
                .and()
                .subject("admin")
                .issuer(jwtProperties.getIssuer())
                .id("invalid-token-id")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(10))))
                .claim("uid", 99L)
                .claim("displayName", "System Admin")
                .claim("roles", List.of("ADMIN"))
                .claim("tokenType", "ACCESS")
                .signWith(Keys.hmacShaKeyFor(CURRENT_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThrows(JwtException.class, () -> jwtTokenProvider.parseAccessToken(invalidToken));
    }

    @Test
    void shouldRejectWeakConfiguredSecret() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("0123456789abcdef0123456789abcdef");

        JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(jwtProperties);

        assertThrows(IllegalStateException.class, jwtTokenProvider::init);
    }

    private JwtProperties createJwtProperties() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setLegacySecret(LEGACY_SECRET);
        jwtProperties.setActiveKid("current");
        jwtProperties.setKeys(List.of(
                signingKey("current", CURRENT_SECRET),
                signingKey("previous", PREVIOUS_SECRET)
        ));
        jwtProperties.setIssuer("ef-transfer-test");
        jwtProperties.setAccessTokenTtl(Duration.ofMinutes(30));
        jwtProperties.setRefreshTokenTtl(Duration.ofDays(7));
        return jwtProperties;
    }

    private JwtProperties.SigningKeyProperties signingKey(String kid, String secret) {
        JwtProperties.SigningKeyProperties key = new JwtProperties.SigningKeyProperties();
        key.setKid(kid);
        key.setSecret(secret);
        return key;
    }
}
