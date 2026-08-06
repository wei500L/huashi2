package com.huashi.eftransfer.app.support;

import com.huashi.eftransfer.app.common.security.lockout.AuthLockoutStore;
import com.huashi.eftransfer.app.common.security.lockout.LocalAuthLockoutStore;
import com.huashi.eftransfer.app.common.security.store.AuthTokenStore;
import com.huashi.eftransfer.app.common.security.store.RegistrationContextSession;
import com.huashi.eftransfer.app.common.security.store.RefreshTokenSession;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@TestConfiguration
public class TestAuthTokenStoreConfiguration {

    @Bean
    @Primary
    public AuthTokenStore authTokenStore() {
        return new InMemoryAuthTokenStore();
    }

    @Bean
    @Primary
    public AuthLockoutStore authLockoutStore() {
        return new LocalAuthLockoutStore();
    }

    @Bean("sessionCompletionTaskExecutor")
    @Primary
    public TaskExecutor sessionCompletionTaskExecutor() {
        return new SyncTaskExecutor();
    }

    @Bean("aiAsyncTaskExecutor")
    @Primary
    public TaskExecutor aiAsyncTaskExecutor() {
        return new SyncTaskExecutor();
    }

    static class InMemoryAuthTokenStore implements AuthTokenStore {

        private final Map<String, RegistrationContextSession> registrationContextSessions = new ConcurrentHashMap<>();
        private final Map<String, Instant> registrationContextLocks = new ConcurrentHashMap<>();
        private final Map<String, RefreshTokenSession> refreshSessions = new ConcurrentHashMap<>();
        private final Map<Long, String> activeUserSessions = new ConcurrentHashMap<>();
        private final Map<String, Instant> blacklistedAccessTokens = new ConcurrentHashMap<>();

        @Override
        public Optional<RegistrationContextSession> findRegistrationContextSession(String tokenHash) {
            RegistrationContextSession session = registrationContextSessions.get(tokenHash);
            if (session == null) {
                return Optional.empty();
            }
            if (session.expiresAt().isBefore(Instant.now())) {
                revokeRegistrationContextSession(tokenHash);
                return Optional.empty();
            }
            return Optional.of(session);
        }

        @Override
        public boolean acquireRegistrationContextLock(String tokenHash, Duration ttl) {
            Instant now = Instant.now();
            AtomicBoolean acquired = new AtomicBoolean(false);
            registrationContextLocks.compute(tokenHash, (key, expiration) -> {
                if (expiration == null || expiration.isBefore(now)) {
                    acquired.set(true);
                    return now.plus(ttl);
                }
                return expiration;
            });
            return acquired.get();
        }

        @Override
        public void releaseRegistrationContextLock(String tokenHash) {
            registrationContextLocks.remove(tokenHash);
        }

        @Override
        public void saveRegistrationContextSession(RegistrationContextSession session, Duration ttl) {
            registrationContextSessions.put(session.tokenHash(), session);
        }

        @Override
        public void revokeRegistrationContextSession(String tokenHash) {
            registrationContextSessions.remove(tokenHash);
            registrationContextLocks.remove(tokenHash);
        }

        @Override
        public Optional<RefreshTokenSession> findRefreshSession(String refreshTokenHash) {
            RefreshTokenSession session = refreshSessions.get(refreshTokenHash);
            if (session == null) {
                return Optional.empty();
            }
            if (session.expiresAt().isBefore(Instant.now())) {
                revokeRefreshSession(refreshTokenHash);
                return Optional.empty();
            }
            return Optional.of(session);
        }

        @Override
        public void saveRefreshSession(RefreshTokenSession session, Duration ttl) {
            refreshSessions.put(session.refreshTokenHash(), session);
            activeUserSessions.put(session.userId(), session.refreshTokenHash());
        }

        @Override
        public void revokeRefreshSession(String refreshTokenHash) {
            RefreshTokenSession session = refreshSessions.remove(refreshTokenHash);
            if (session != null) {
                activeUserSessions.remove(session.userId(), refreshTokenHash);
            }
        }

        @Override
        public Optional<String> findActiveRefreshTokenHash(Long userId) {
            return Optional.ofNullable(activeUserSessions.get(userId));
        }

        @Override
        public void revokeAllUserSessions(Long userId) {
            Optional.ofNullable(activeUserSessions.remove(userId)).ifPresent(refreshSessions::remove);
        }

        @Override
        public void blacklistAccessToken(String tokenId, Duration ttl) {
            if (ttl.isNegative() || ttl.isZero()) {
                return;
            }
            blacklistedAccessTokens.put(tokenId, Instant.now().plus(ttl));
        }

        @Override
        public boolean isAccessTokenBlacklisted(String tokenId) {
            Instant expiration = blacklistedAccessTokens.get(tokenId);
            if (expiration == null) {
                return false;
            }
            if (expiration.isBefore(Instant.now())) {
                blacklistedAccessTokens.remove(tokenId);
                return false;
            }
            return true;
        }
    }
}
