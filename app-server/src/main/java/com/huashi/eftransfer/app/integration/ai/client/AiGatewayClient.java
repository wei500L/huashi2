package com.huashi.eftransfer.app.integration.ai.client;

import com.huashi.eftransfer.app.integration.ai.dto.AiGatewayHealthResponse;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Component
public class AiGatewayClient {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayClient.class);
    private static final ParameterizedTypeReference<ApiResponse<AiGatewayHealthResponse>> HEALTH_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient aiGatewayRestClient;

    public AiGatewayClient(RestClient aiGatewayRestClient) {
        this.aiGatewayRestClient = aiGatewayRestClient;
    }

    public Optional<AiGatewayHealthResponse> fetchHealth() {
        try {
            ApiResponse<AiGatewayHealthResponse> response = aiGatewayRestClient.get()
                    .uri("/internal/ai/health")
                    .retrieve()
                    .body(HEALTH_TYPE);

            if (response == null || !response.success()) {
                return Optional.empty();
            }
            return Optional.ofNullable(response.data());
        } catch (RestClientException ex) {
            log.warn("event=ai_gateway_unreachable reason={}", ex.getMessage());
            return Optional.empty();
        }
    }
}
