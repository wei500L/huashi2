package com.huashi.eftransfer.app.common.security.lockout;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class RedisAuthLockoutStore implements AuthLockoutStore {

    private static final String FAILURE_PREFIX = "auth:lockout:failures:";
    private static final String LOCK_PREFIX = "auth:lockout:locked:";

    private final StringRedisTemplate stringRedisTemplate;

    public RedisAuthLockoutStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Optional<Duration> remainingLockDuration(String principalKey) {
        Long seconds = stringRedisTemplate.getExpire(lockKey(principalKey));
        if (seconds == null || seconds <= 0) {
            return Optional.empty();
        }
        return Optional.of(Duration.ofSeconds(seconds));
    }

    @Override
    public long incrementFailures(String principalKey, Duration ttl) {
        String key = failureKey(principalKey);
        Long failures = stringRedisTemplate.opsForValue().increment(key);
        stringRedisTemplate.expire(key, ttl);
        return failures == null ? 0L : failures;
    }

    @Override
    public void lock(String principalKey, Duration ttl) {
        stringRedisTemplate.opsForValue().set(lockKey(principalKey), "1", ttl);
        stringRedisTemplate.delete(failureKey(principalKey));
    }

    @Override
    public void reset(String principalKey) {
        stringRedisTemplate.delete(List.of(failureKey(principalKey), lockKey(principalKey)));
    }

    private String failureKey(String principalKey) {
        return FAILURE_PREFIX + principalKey;
    }

    private String lockKey(String principalKey) {
        return LOCK_PREFIX + principalKey;
    }
}
