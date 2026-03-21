package com.huashi.eftransfer.app.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.security.rate-limit.auth")
public class AuthRateLimitProperties {

    private boolean enabled = true;

    @Valid
    private final LoginProperties login = new LoginProperties();

    @Valid
    private final RefreshProperties refresh = new RefreshProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LoginProperties getLogin() {
        return login;
    }

    public RefreshProperties getRefresh() {
        return refresh;
    }

    public static class LoginProperties {

        @Valid
        private final RateLimitWindow ip = new RateLimitWindow(20, Duration.ofMinutes(1));

        @Valid
        private final RateLimitWindow principal = new RateLimitWindow(10, Duration.ofMinutes(10));

        public RateLimitWindow getIp() {
            return ip;
        }

        public RateLimitWindow getPrincipal() {
            return principal;
        }
    }

    public static class RefreshProperties {

        @Valid
        private final RateLimitWindow ip = new RateLimitWindow(40, Duration.ofMinutes(1));

        @Valid
        private final RateLimitWindow session = new RateLimitWindow(20, Duration.ofMinutes(10));

        public RateLimitWindow getIp() {
            return ip;
        }

        public RateLimitWindow getSession() {
            return session;
        }
    }

    public static class RateLimitWindow {

        @Min(1)
        private long limit;

        @NotNull
        private Duration window;

        public RateLimitWindow() {
        }

        public RateLimitWindow(long limit, Duration window) {
            this.limit = limit;
            this.window = window;
        }

        public long getLimit() {
            return limit;
        }

        public void setLimit(long limit) {
            this.limit = limit;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }
}
