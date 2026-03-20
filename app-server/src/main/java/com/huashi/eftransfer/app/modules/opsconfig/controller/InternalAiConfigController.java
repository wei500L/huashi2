package com.huashi.eftransfer.app.modules.opsconfig.controller;

import com.huashi.eftransfer.app.modules.internal.service.InternalKnowledgeService;
import com.huashi.eftransfer.app.modules.opsconfig.service.AiOpsAdminService;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ops/ai-config")
public class InternalAiConfigController {

    private final InternalKnowledgeService internalKnowledgeService;
    private final AiOpsAdminService aiOpsAdminService;

    public InternalAiConfigController(
            InternalKnowledgeService internalKnowledgeService,
            AiOpsAdminService aiOpsAdminService
    ) {
        this.internalKnowledgeService = internalKnowledgeService;
        this.aiOpsAdminService = aiOpsAdminService;
    }

    @GetMapping
    public ApiResponse<AiOpsConfigEffectiveResponse> getStoredConfig(
            @RequestHeader(name = "X-Internal-Token", required = false) String internalToken
    ) {
        internalKnowledgeService.validateToken(internalToken);
        return ApiResponse.success(aiOpsAdminService.getStoredConfigForInternalSync(), MDC.get("traceId"));
    }
}
