package com.huashi.eftransfer.app.modules.opsconfig.controller;

import com.huashi.eftransfer.app.modules.opsconfig.service.AiOpsAdminService;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ops/ai-config")
public class InternalAiConfigController {

    private final AiOpsAdminService aiOpsAdminService;

    public InternalAiConfigController(
            AiOpsAdminService aiOpsAdminService
    ) {
        this.aiOpsAdminService = aiOpsAdminService;
    }

    @GetMapping
    public ApiResponse<AiOpsConfigEffectiveResponse> getStoredConfig() {
        return ApiResponse.success(aiOpsAdminService.getStoredConfigForInternalSync(), MDC.get("traceId"));
    }
}
