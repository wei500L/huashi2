package com.huashi.eftransfer.app.common.security.ratelimit;

import com.huashi.eftransfer.app.common.config.AuthRateLimitProperties;
import com.huashi.eftransfer.app.common.security.store.AuthTokenStore;
import com.huashi.eftransfer.app.common.util.TokenGenerator;
import com.huashi.eftransfer.app.modules.auth.dto.LoginRequest;
import com.huashi.eftransfer.app.modules.auth.dto.RefreshTokenRequest;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AuthRequestRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AuthRequestRateLimiter.class);
    private static final int TOO_MANY_REQUESTS = 429;

    private final AuthRateLimitProperties properties;
    private final AuthRateLimitBucketResolver bucketResolver;
    private final AuthTokenStore authTokenStore;

    public AuthRequestRateLimiter(
            AuthRateLimitProperties properties,
            AuthRateLimitBucketResolver bucketResolver,
            AuthTokenStore authTokenStore
    ) {
        this.properties = properties;
        this.bucketResolver = bucketResolver;
        this.authTokenStore = authTokenStore;
    }

    public void checkLogin(HttpServletRequest request, LoginRequest loginRequest) {
        if (!properties.isEnabled()) {
            return;
        }
        consume(
                "auth:rl:login:ip:" + remoteAddress(request),
                properties.getLogin().getIp(),
                "Too many login attempts",
                request,
                "login",
                "ip"
        );
        consume(
                "auth:rl:login:principal:" + normalizeLoginId(loginRequest.usernameOrEmail()),
                properties.getLogin().getPrincipal(),
                "Too many login attempts",
                request,
                "login",
                "principal"
        );
    }

    public void checkRefresh(HttpServletRequest request, RefreshTokenRequest refreshRequest) {
        if (!properties.isEnabled()) {
            return;
        }
        consume(
                "auth:rl:refresh:ip:" + remoteAddress(request),
                properties.getRefresh().getIp(),
                "Too many refresh attempts",
                request,
                "refresh",
                "ip"
        );
        consume(
                "auth:rl:refresh:session:" + refreshSessionKey(refreshRequest.refreshToken()),
                properties.getRefresh().getSession(),
                "Too many refresh attempts",
                request,
                "refresh",
                "session"
        );
    }

    private void consume(
            String key,
            AuthRateLimitProperties.RateLimitWindow limit,
            String message,
            HttpServletRequest request,
            String action,
            String dimension
    ) {
        if (bucketResolver.resolve(key, limit).tryConsume(1)) {
            return;
        }
        log.warn(
                "event=auth_rate_limited action={} dimension={} method={} path={} remoteAddr={}",
                action,
                dimension,
                request.getMethod(),
                request.getRequestURI(),
                remoteAddress(request)
        );
        throw new BusinessException(ResultCode.RATE_LIMITED, message, TOO_MANY_REQUESTS);
    }

    private String normalizeLoginId(String loginId) {
        return loginId == null ? "" : loginId.trim().toLowerCase(Locale.ROOT);
    }

    private String refreshSessionKey(String refreshToken) {
        String refreshTokenHash = TokenGenerator.sha256(refreshToken);
        return authTokenStore.findRefreshSession(refreshTokenHash)
                .map(session -> String.valueOf(session.userId()))
                .orElse(refreshTokenHash);
    }

    private String remoteAddress(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
    }
}
