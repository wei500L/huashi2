package com.huashi.eftransfer.app.common.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.security.registration")
public class AuthRegistrationProperties {

    @NotNull
    private Duration contextTokenTtl = Duration.ofMinutes(10);

    @NotNull
    private Duration contextLockTtl = Duration.ofMinutes(1);

    public Duration getContextTokenTtl() {
        return contextTokenTtl;
    }

    public void setContextTokenTtl(Duration contextTokenTtl) {
        this.contextTokenTtl = contextTokenTtl;
    }

    public Duration getContextLockTtl() {
        return contextLockTtl;
    }

    public void setContextLockTtl(Duration contextLockTtl) {
        this.contextLockTtl = contextLockTtl;
    }
}
