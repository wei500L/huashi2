package com.huashi.eftransfer.ai.modules.health.controller;

import com.huashi.eftransfer.ai.modules.health.service.AiHealthService;
import com.huashi.eftransfer.shared.ai.AiGatewayHealthResponse;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai/health")
public class AiHealthController {

    private final AiHealthService aiHealthService;

    public AiHealthController(AiHealthService aiHealthService) {
        this.aiHealthService = aiHealthService;
    }

    @GetMapping
    public ApiResponse<AiGatewayHealthResponse> health() {
        return ApiResponse.success(aiHealthService.getHealthPayload(), MDC.get("traceId"));
    }
}
