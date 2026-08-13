package com.huashi.eftransfer.app.common.config;

import jakarta.validation.Valid;
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

    @Valid
    private final RegisterProperties register = new RegisterProperties();

    @Valid
    private final RegisterContextProperties registerContext = new RegisterContextProperties();

    @Valid
    private final ChangePasswordProperties changePassword = new ChangePasswordProperties();

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

    public RegisterProperties getRegister() {
        return register;
    }

    public RegisterContextProperties getRegisterContext() {
        return registerContext;
    }

    public ChangePasswordProperties getChangePassword() {
        return changePassword;
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

    public static class RegisterProperties {

        @Valid
        private final RateLimitWindow ip = new RateLimitWindow(10, Duration.ofMinutes(10));

        public RateLimitWindow getIp() {
            return ip;
        }
    }

    public static class RegisterContextProperties {

        @Valid
        private final RateLimitWindow ip = new RateLimitWindow(30, Duration.ofMinutes(1));

        public RateLimitWindow getIp() {
            return ip;
        }
    }

    public static class ChangePasswordProperties {

        @Valid
        private final RateLimitWindow ip = new RateLimitWindow(10, Duration.ofMinutes(1));

        @Valid
        private final RateLimitWindow user = new RateLimitWindow(5, Duration.ofMinutes(10));

        public RateLimitWindow getIp() {
            return ip;
        }

        public RateLimitWindow getUser() {
            return user;
        }
    }
}
