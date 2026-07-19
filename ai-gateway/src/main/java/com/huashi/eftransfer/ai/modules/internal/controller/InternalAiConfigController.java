package com.huashi.eftransfer.ai.modules.internal.controller;

import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.ai.modules.internal.service.AiConfigProbeService;
import com.huashi.eftransfer.shared.ai.AdminAiChatProbeVO;
import com.huashi.eftransfer.shared.ai.AdminAiEmbeddingProbeVO;
import com.huashi.eftransfer.shared.ai.AdminAiRerankProbeVO;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigApplyRequest;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigApplyResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigCommitRequest;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigEffectiveResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigPayload;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigStageRequest;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigStageResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigValidationResponse;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai/config")
public class InternalAiConfigController {

    private final AiRuntimeConfigService runtimeConfigService;
    private final AiConfigProbeService aiConfigProbeService;

    public InternalAiConfigController(AiRuntimeConfigService runtimeConfigService, AiConfigProbeService aiConfigProbeService) {
        this.runtimeConfigService = runtimeConfigService;
        this.aiConfigProbeService = aiConfigProbeService;
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

    @PostMapping("/stage")
    public ApiResponse<AiOpsConfigStageResponse> stage(@RequestBody AiOpsConfigStageRequest request) {
        String source = StringUtils.hasText(request.source()) ? request.source() : "ADMIN_STAGE";
        aiConfigProbeService.requireReady(request.config());
        return ApiResponse.success(runtimeConfigService.stage(request.config(), source, request.version()), MDC.get("traceId"));
    }

    @PostMapping("/commit")
    public ApiResponse<AiOpsConfigApplyResponse> commit(@RequestBody AiOpsConfigCommitRequest request) {
        return ApiResponse.success(runtimeConfigService.commit(request.stageId()), MDC.get("traceId"));
    }

    @PostMapping("/probes/embedding")
    public ApiResponse<AdminAiEmbeddingProbeVO> probeEmbedding(@RequestBody AiOpsConfigPayload request) {
        return ApiResponse.success(aiConfigProbeService.probeEmbedding(request), MDC.get("traceId"));
    }

    @PostMapping("/probes/chat")
    public ApiResponse<AdminAiChatProbeVO> probeChat(@RequestBody AiOpsConfigPayload request) {
        return ApiResponse.success(aiConfigProbeService.probeChat(request), MDC.get("traceId"));
    }

    @PostMapping("/probes/rerank")
    public ApiResponse<AdminAiRerankProbeVO> probeRerank(@RequestBody AiOpsConfigPayload request) {
        return ApiResponse.success(aiConfigProbeService.probeRerank(request), MDC.get("traceId"));
    }
}
