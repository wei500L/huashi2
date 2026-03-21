package com.huashi.eftransfer.app.common.security;

import com.huashi.eftransfer.app.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    private static final ObjectMapper HEADER_OBJECT_MAPPER = new ObjectMapper();
    private final JwtProperties jwtProperties;
    private Map<String, SecretKey> verificationKeys;
    private SecretKey activeSigningKey;
    private String activeKid;
    private SecretKey legacySigningKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        Map<String, SecretKey> configuredKeys = new LinkedHashMap<>();
        for (JwtProperties.SigningKeyProperties configuredKey : jwtProperties.resolveConfiguredKeys()) {
            configuredKeys.put(configuredKey.getKid(), signingKey(configuredKey.getSecret(), "JWT secret for kid=" + configuredKey.getKid()));
        }
        if (configuredKeys.isEmpty()) {
            throw new IllegalStateException("At least one JWT signing key must be configured");
        }
        this.verificationKeys = Map.copyOf(configuredKeys);
        this.activeKid = jwtProperties.resolveActiveKid();
        this.activeSigningKey = verificationKeys.get(activeKid);
        if (activeSigningKey == null) {
            throw new IllegalStateException("JWT active kid does not match any configured signing key: " + activeKid);
        }

        String legacySecret = jwtProperties.resolveLegacySecret();
        this.legacySigningKey = legacySecret == null || legacySecret.isBlank()
                ? null
                : signingKey(legacySecret, "JWT legacy secret");
    }

    public AccessToken generateAccessToken(Long userId, String username, String displayName, Set<String> roles) {
        Instant now = Instant.now();
        Instant expiration = now.plus(jwtProperties.getAccessTokenTtl());
        String tokenId = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .header()
                .add("kid", activeKid)
                .and()
                .subject(username)
                .issuer(jwtProperties.getIssuer())
                .id(tokenId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_DISPLAY_NAME, displayName)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TOKEN_TYPE, ACCESS_TOKEN_TYPE)
                .signWith(activeSigningKey)
                .compact();
        return new AccessToken(token, tokenId, expiration);
    }

    public JwtPrincipal parseAccessToken(String token) {
        SecretKey verificationKey = resolveVerificationKey(token);
        Claims claims = Jwts.parser()
                .verifyWith(verificationKey)
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

    private SecretKey resolveVerificationKey(String token) {
        String keyId = resolveKeyId(token);
        if (keyId == null || keyId.isBlank()) {
            if (legacySigningKey == null) {
                throw new JwtException("JWT header does not contain kid and no legacy secret is configured");
            }
            return legacySigningKey;
        }
        SecretKey key = verificationKeys.get(keyId);
        if (key == null) {
            throw new JwtException("JWT kid is not recognized: " + keyId);
        }
        return key;
    }

    private String resolveKeyId(String token) {
        try {
            String[] segments = token.split("\\.");
            if (segments.length < 2) {
                throw new JwtException("JWT format is invalid");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> header = HEADER_OBJECT_MAPPER.readValue(
                    Base64.getUrlDecoder().decode(segments[0]),
                    Map.class
            );
            Object keyId = header.get("kid");
            return keyId instanceof String value ? value : null;
        } catch (JwtException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new JwtException("Failed to parse JWT header", exception);
        }
    }

    private SecretKey signingKey(String secret, String description) {
        JwtSecretValidator.validate(secret, description);
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
