package com.huashi.eftransfer.app.common.config;

import jakarta.validation.Valid;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.security.rate-limit.assessment")
public class AssessmentRateLimitProperties {

    private boolean enabled = true;

    @Valid
    private final VerifyProperties verify = new VerifyProperties();

    @Valid
    private final QrEntryProperties qrEntry = new QrEntryProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public VerifyProperties getVerify() {
        return verify;
    }

    public QrEntryProperties getQrEntry() {
        return qrEntry;
    }

    public static class VerifyProperties {

        @Valid
        private final RateLimitWindow ip = new RateLimitWindow(10, Duration.ofMinutes(10));

        public RateLimitWindow getIp() {
            return ip;
        }
    }

    public static class QrEntryProperties {

        @Valid
        private final RateLimitWindow ip = new RateLimitWindow(10, Duration.ofMinutes(10));

        public RateLimitWindow getIp() {
            return ip;
        }
    }
}
