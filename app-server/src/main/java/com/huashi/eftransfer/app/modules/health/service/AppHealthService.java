package com.huashi.eftransfer.app.modules.health.service;

import com.huashi.eftransfer.app.common.config.AiGatewayClientProperties;
import com.huashi.eftransfer.app.modules.health.dto.AppHealthPayload;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class AppHealthService {

    private final Environment environment;
    private final AiGatewayClientProperties aiGatewayClientProperties;

    public AppHealthService(Environment environment, AiGatewayClientProperties aiGatewayClientProperties) {
        this.environment = environment;
        this.aiGatewayClientProperties = aiGatewayClientProperties;
    }

    public AppHealthPayload getHealthPayload() {
        List<String> profiles = Arrays.asList(environment.getActiveProfiles());
        return new AppHealthPayload(
                "app-server",
                "UP",
                profiles.isEmpty() ? List.of("default") : profiles,
                aiGatewayClientProperties.getBaseUrl(),
                OffsetDateTime.now()
        );
    }
}
