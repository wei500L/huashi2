package com.huashi.eftransfer.app.common.security.store;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnMissingBean(AuthTokenStore.class)
public class RedisAuthTokenStore implements AuthTokenStore {

    private static final String REFRESH_PREFIX = "auth:refresh:";
    private static final String USER_SESSION_PREFIX = "auth:user-session:";
    private static final String ACCESS_BLACKLIST_PREFIX = "auth:access-blacklist:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAuthTokenStore(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<RefreshTokenSession> findRefreshSession(String refreshTokenHash) {
        String payload = stringRedisTemplate.opsForValue().get(refreshKey(refreshTokenHash));
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, RefreshTokenSession.class));
        } catch (JacksonException exception) {
            stringRedisTemplate.delete(refreshKey(refreshTokenHash));
            return Optional.empty();
        }
    }

    @Override
    public void saveRefreshSession(RefreshTokenSession session, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(
                    refreshKey(session.refreshTokenHash()),
                    objectMapper.writeValueAsString(session),
                    ttl
            );
            stringRedisTemplate.opsForValue().set(userSessionKey(session.userId()), session.refreshTokenHash(), ttl);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize refresh token session", exception);
        }
    }

    @Override
    public void revokeRefreshSession(String refreshTokenHash) {
        findRefreshSession(refreshTokenHash).ifPresent(session -> stringRedisTemplate.delete(userSessionKey(session.userId())));
        stringRedisTemplate.delete(refreshKey(refreshTokenHash));
    }

    @Override
    public Optional<String> findActiveRefreshTokenHash(Long userId) {
        return Optional.ofNullable(stringRedisTemplate.opsForValue().get(userSessionKey(userId)));
    }

    @Override
    public void revokeAllUserSessions(Long userId) {
        findActiveRefreshTokenHash(userId).ifPresent(this::revokeRefreshSession);
        stringRedisTemplate.delete(userSessionKey(userId));
    }

    @Override
    public void blacklistAccessToken(String tokenId, Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        stringRedisTemplate.opsForValue().set(accessBlacklistKey(tokenId), "1", ttl);
    }

    @Override
    public boolean isAccessTokenBlacklisted(String tokenId) {
        Boolean exists = stringRedisTemplate.hasKey(accessBlacklistKey(tokenId));
        return Boolean.TRUE.equals(exists);
    }

    private String refreshKey(String refreshTokenHash) {
        return REFRESH_PREFIX + refreshTokenHash;
    }

    private String userSessionKey(Long userId) {
        return USER_SESSION_PREFIX + userId;
    }

    private String accessBlacklistKey(String tokenId) {
        return ACCESS_BLACKLIST_PREFIX + tokenId;
    }
}
