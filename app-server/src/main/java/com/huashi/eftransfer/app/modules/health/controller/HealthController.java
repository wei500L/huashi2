package com.huashi.eftransfer.app.modules.health.controller;

import com.huashi.eftransfer.app.modules.health.dto.AppHealthPayload;
import com.huashi.eftransfer.app.modules.health.service.AppHealthService;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final AppHealthService appHealthService;

    public HealthController(AppHealthService appHealthService) {
        this.appHealthService = appHealthService;
    }

    @GetMapping
    public ApiResponse<AppHealthPayload> health() {
        return ApiResponse.success(appHealthService.getHealthPayload(), MDC.get("traceId"));
    }
}
