package com.huashi.eftransfer.app.common.security.store;

import java.time.Duration;
import java.util.Optional;

public interface AuthTokenStore {

    Optional<RegistrationContextSession> findRegistrationContextSession(String tokenHash);

    boolean acquireRegistrationContextLock(String tokenHash, Duration ttl);

    void releaseRegistrationContextLock(String tokenHash);

    void saveRegistrationContextSession(RegistrationContextSession session, Duration ttl);

    void revokeRegistrationContextSession(String tokenHash);

    Optional<RefreshTokenSession> findRefreshSession(String refreshTokenHash);

    void saveRefreshSession(RefreshTokenSession session, Duration ttl);

    void revokeRefreshSession(String refreshTokenHash);

    Optional<String> findActiveRefreshTokenHash(Long userId);

    void revokeAllUserSessions(Long userId);

    void blacklistAccessToken(String tokenId, Duration ttl);

    boolean isAccessTokenBlacklisted(String tokenId);
}
