package com.huashi.eftransfer.app.common.security.lockout;

import java.time.Duration;
import java.util.Optional;

public interface AuthLockoutStore {

    Optional<Duration> remainingLockDuration(String principalKey);

    long incrementFailures(String principalKey, Duration ttl);

    void lock(String principalKey, Duration ttl);

    void reset(String principalKey);
}
