package com.huashi.eftransfer.app.common.config;

import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.security.rate-limit.ai")
public class AiRateLimitProperties {

    private boolean enabled = true;

    @Valid
    private final RateLimitWindow user = new RateLimitWindow(30, Duration.ofMinutes(10));

    @Valid
    private final RateLimitWindow ip = new RateLimitWindow(200, Duration.ofMinutes(10));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RateLimitWindow getUser() {
        return user;
    }

    public RateLimitWindow getIp() {
        return ip;
    }
}
