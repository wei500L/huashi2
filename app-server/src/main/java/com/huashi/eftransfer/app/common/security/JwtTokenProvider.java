package com.huashi.eftransfer.app.common.security;

import com.huashi.eftransfer.app.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private Key signingKey;

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

    public String generateToken(String username, String role) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getAccessTokenTtl());
        return Jwts.builder()
                .subject(username)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim("role", role)
                .signWith(signingKey)
                .compact();
    }

    public JwtPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        Collection<? extends GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        return new JwtPrincipal(username, role, authorities);
    }
}
