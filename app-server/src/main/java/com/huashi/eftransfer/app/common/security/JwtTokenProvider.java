package com.huashi.eftransfer.app.common.security;

import com.huashi.eftransfer.app.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_DISPLAY_NAME = "displayName";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private final JwtProperties jwtProperties;
    private SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        if (jwtProperties.getSecret().length() < 32) {
            throw new IllegalStateException("JWT secret must contain at least 32 characters");
        }
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public AccessToken generateAccessToken(Long userId, String username, String displayName, Set<String> roles) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getAccessTokenTtl());
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .subject(username)
                .issuer(jwtProperties.getIssuer())
                .id(tokenId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_DISPLAY_NAME, displayName)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TOKEN_TYPE, ACCESS_TOKEN_TYPE)
                .signWith(signingKey)
                .compact();
        return new AccessToken(token, tokenId, expiration);
    }

    public JwtPrincipal parseAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String tokenType = claims.get(CLAIM_TOKEN_TYPE, String.class);
        if (!ACCESS_TOKEN_TYPE.equals(tokenType)) {
            throw new io.jsonwebtoken.JwtException("Unexpected token type: " + tokenType);
        }

        Long userId = claims.get(CLAIM_USER_ID, Long.class);
        String username = claims.getSubject();
        String displayName = claims.get(CLAIM_DISPLAY_NAME, String.class);
        @SuppressWarnings("unchecked")
        List<String> roleList = claims.get(CLAIM_ROLES, List.class);
        Set<String> roles = roleList == null ? Set.of() : Set.copyOf(roleList);
        Collection<? extends GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toSet());
        return new JwtPrincipal(
                userId,
                username,
                displayName,
                roles,
                claims.getId(),
                claims.getExpiration().toInstant(),
                authorities
        );
    }
}
