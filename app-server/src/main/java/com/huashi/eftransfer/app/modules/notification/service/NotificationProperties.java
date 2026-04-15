package com.huashi.eftransfer.app.modules.notification.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    private final HealthMonitor healthMonitor = new HealthMonitor();

    public HealthMonitor getHealthMonitor() {
        return healthMonitor;
    }

    public static class HealthMonitor {

        private boolean enabled = true;

        private Duration pollInterval = Duration.ofMinutes(5);

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Duration getPollInterval() {
            return pollInterval;
        }

        public void setPollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
        }
    }
}
