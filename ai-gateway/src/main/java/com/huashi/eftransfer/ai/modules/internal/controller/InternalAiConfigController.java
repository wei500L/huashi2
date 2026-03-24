package com.huashi.eftransfer.ai.modules.internal.controller;

import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigApplyRequest;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigApplyResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigValidationResponse;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai/config")
public class InternalAiConfigController {

    private final AiRuntimeConfigService runtimeConfigService;

    public InternalAiConfigController(AiRuntimeConfigService runtimeConfigService) {
        this.runtimeConfigService = runtimeConfigService;
    }

    @GetMapping("/effective")
    public ApiResponse<AiOpsConfigEffectiveResponse> effective() {
        return ApiResponse.success(runtimeConfigService.effective(), MDC.get("traceId"));
    }

    @PostMapping("/validate")
    public ApiResponse<AiOpsConfigValidationResponse> validate(@RequestBody AiOpsConfigPayload request) {
        return ApiResponse.success(runtimeConfigService.validate(request), MDC.get("traceId"));
    }

    @PostMapping("/apply")
    public ApiResponse<AiOpsConfigApplyResponse> apply(@RequestBody AiOpsConfigApplyRequest request) {
        String source = StringUtils.hasText(request.source()) ? request.source() : "ADMIN_APPLY";
        AiOpsConfigEffectiveResponse response = runtimeConfigService.apply(request.config(), source, request.version());
        return ApiResponse.success(
                new AiOpsConfigApplyResponse(response.source(), response.version(), response.appliedAt(), response.notices()),
                MDC.get("traceId")
        );
    }
}
