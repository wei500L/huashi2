package com.huashi.eftransfer.app.modules.auth.service;

import com.huashi.eftransfer.app.common.config.AuthLockoutProperties;
import com.huashi.eftransfer.app.common.security.lockout.AuthLockoutStore;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;

@Service
public class AuthLockoutService {

    private static final Logger log = LoggerFactory.getLogger(AuthLockoutService.class);
    private static final int LOCKED_STATUS = 423;

    private final AuthLockoutProperties properties;
    private final AuthLockoutStore authLockoutStore;

    public AuthLockoutService(AuthLockoutProperties properties, AuthLockoutStore authLockoutStore) {
        this.properties = properties;
        this.authLockoutStore = authLockoutStore;
    }

    public void ensureNotLocked(UserEntity user, String loginId) {
        if (!properties.isEnabled()) {
            return;
        }
        String principalKey = principalKey(user, loginId);
        authLockoutStore.remainingLockDuration(principalKey)
                .ifPresent(duration -> {
                    log.warn("event=auth_lockout_blocked principalKey={} remainingSeconds={}",
                            principalKey,
                            duration.toSeconds());
                    throw accountLocked(duration);
                });
    }

    public void recordFailure(UserEntity user, String loginId) {
        if (!properties.isEnabled()) {
            return;
        }
        String principalKey = principalKey(user, loginId);
        long failures = authLockoutStore.incrementFailures(principalKey, properties.getDuration());
        if (failures < properties.getThreshold()) {
            return;
        }
        authLockoutStore.lock(principalKey, properties.getDuration());
        log.warn("event=auth_lockout_activated principalKey={} threshold={} durationSeconds={}",
                principalKey,
                properties.getThreshold(),
                properties.getDuration().toSeconds());
    }

    public void clearFailures(UserEntity user, String loginId) {
        if (!properties.isEnabled()) {
            return;
        }
        authLockoutStore.reset(principalKey(user, loginId));
    }

    private String principalKey(UserEntity user, String loginId) {
        if (user != null && user.getId() != null) {
            return "user:" + user.getId();
        }
        return "login:" + normalizeLoginId(loginId);
    }

    private String normalizeLoginId(String loginId) {
        return loginId == null ? "" : loginId.trim().toLowerCase(Locale.ROOT);
    }

    private BusinessException accountLocked(Duration remainingDuration) {
        long minutes = Math.max(1, remainingDuration.toMinutes());
        return new BusinessException(
                ResultCode.ACCOUNT_LOCKED,
                "Account is temporarily locked. Try again in " + minutes + " minute(s).",
                LOCKED_STATUS
        );
    }
}
