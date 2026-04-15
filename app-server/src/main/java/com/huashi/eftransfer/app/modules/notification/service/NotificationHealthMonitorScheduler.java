package com.huashi.eftransfer.app.modules.notification.service;

import com.huashi.eftransfer.app.integration.ai.client.AiGatewayClient;
import com.huashi.eftransfer.app.integration.ai.dto.AiGatewayHealthResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class NotificationHealthMonitorScheduler {

    private final NotificationProperties notificationProperties;
    private final AiGatewayClient aiGatewayClient;
    private final NotificationScenarioService notificationScenarioService;
    private final AtomicBoolean previouslyHealthy = new AtomicBoolean(true);

    public NotificationHealthMonitorScheduler(
            NotificationProperties notificationProperties,
            AiGatewayClient aiGatewayClient,
            NotificationScenarioService notificationScenarioService
    ) {
        this.notificationProperties = notificationProperties;
        this.aiGatewayClient = aiGatewayClient;
        this.notificationScenarioService = notificationScenarioService;
    }

    @Scheduled(fixedDelayString = "#{@notificationProperties.healthMonitor.pollInterval.toMillis()}")
    public void checkAiGatewayHealth() {
        if (!notificationProperties.getHealthMonitor().isEnabled()) {
            return;
        }
        Optional<AiGatewayHealthResponse> healthOptional = aiGatewayClient.fetchHealth();
        boolean healthy = healthOptional
                .map(this::isHealthy)
                .orElse(false);

        boolean wasHealthy = previouslyHealthy.getAndSet(healthy);
        if (!healthy && wasHealthy) {
            notificationScenarioService.notifyAiGatewayUnhealthy(resolveReason(healthOptional.orElse(null)));
            return;
        }
        if (healthy && !wasHealthy) {
            notificationScenarioService.notifyAiGatewayRecovered(healthOptional.orElse(null));
        }
    }

    private boolean isHealthy(AiGatewayHealthResponse health) {
        if (health == null) {
            return false;
        }
        return "UP".equalsIgnoreCase(health.status())
                && health.databaseReady()
                && health.vectorStoreReady()
                && health.providerReady()
                && health.rerankReady()
                && health.appServerReady();
    }

    private String resolveReason(AiGatewayHealthResponse health) {
        if (health == null) {
            return "ai-gateway unavailable";
        }
        if (!"UP".equalsIgnoreCase(health.status())) {
            return "status=" + health.status();
        }
        if (!health.providerReady()) {
            return "provider unavailable";
        }
        if (!health.vectorStoreReady()) {
            return "vector store unavailable";
        }
        if (!health.databaseReady()) {
            return "database unavailable";
        }
        if (!health.rerankReady()) {
            return "rerank unavailable";
        }
        if (!health.appServerReady()) {
            return health.appServerError() == null ? "app-server integration unavailable" : health.appServerError();
        }
        return "unknown";
    }
}
