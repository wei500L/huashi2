package com.huashi.eftransfer.app.common.security.lockout;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class LocalAuthLockoutStore implements AuthLockoutStore {

    private final Map<String, LockoutState> states = new ConcurrentHashMap<>();

    @Override
    public synchronized Optional<Duration> remainingLockDuration(String principalKey) {
        LockoutState state = states.get(principalKey);
        if (state == null) {
            return Optional.empty();
        }
        Instant now = Instant.now();
        if (state.lockExpiresAt != null && !state.lockExpiresAt.isAfter(now)) {
            state.lockExpiresAt = null;
        }
        if (state.failureExpiresAt != null && !state.failureExpiresAt.isAfter(now)) {
            state.failureCount = 0;
            state.failureExpiresAt = null;
        }
        prune(principalKey, state);
        if (state.lockExpiresAt == null) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(now, state.lockExpiresAt));
    }

    @Override
    public synchronized long incrementFailures(String principalKey, Duration ttl) {
        LockoutState state = states.computeIfAbsent(principalKey, ignored -> new LockoutState());
        Instant now = Instant.now();
        if (state.failureExpiresAt == null || !state.failureExpiresAt.isAfter(now)) {
            state.failureCount = 0;
        }
        state.failureCount += 1;
        state.failureExpiresAt = now.plus(ttl);
        if (state.lockExpiresAt != null && !state.lockExpiresAt.isAfter(now)) {
            state.lockExpiresAt = null;
        }
        return state.failureCount;
    }

    @Override
    public synchronized void lock(String principalKey, Duration ttl) {
        LockoutState state = states.computeIfAbsent(principalKey, ignored -> new LockoutState());
        state.failureCount = 0;
        state.failureExpiresAt = null;
        state.lockExpiresAt = Instant.now().plus(ttl);
    }

    @Override
    public synchronized void reset(String principalKey) {
        states.remove(principalKey);
    }

    private void prune(String principalKey, LockoutState state) {
        if (state.failureCount == 0 && state.lockExpiresAt == null) {
            states.remove(principalKey);
        }
    }

    private static final class LockoutState {
        private long failureCount;
        private Instant failureExpiresAt;
        private Instant lockExpiresAt;
    }
}
