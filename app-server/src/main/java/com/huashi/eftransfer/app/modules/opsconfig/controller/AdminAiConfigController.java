package com.huashi.eftransfer.app.modules.opsconfig.controller;

import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiConfigSaveRequest;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiConfigDriftVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiConfigViewVO;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminAiRuntimeSyncRequest;
import com.huashi.eftransfer.app.modules.opsconfig.dto.AdminOutboxRecordVO;
import com.huashi.eftransfer.app.modules.opsconfig.service.AiOpsAdminService;
import com.huashi.eftransfer.shared.ai.AiGatewayHealthResponse;
import com.huashi.eftransfer.shared.ai.AdminAiEmbeddingProbeVO;
import com.huashi.eftransfer.shared.ai.AdminAiRerankProbeVO;
import com.huashi.eftransfer.shared.ai.RagReindexJobResponse;
import com.huashi.eftransfer.shared.ai.RagReindexRequest;
import com.huashi.eftransfer.shared.ai.RagReindexResponse;
import com.huashi.eftransfer.shared.ai.config.AiOpsConfigValidationResponse;
import com.huashi.eftransfer.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ai-config")
public class AdminAiConfigController {

    private final AiOpsAdminService aiOpsAdminService;

    public AdminAiConfigController(AiOpsAdminService aiOpsAdminService) {
        this.aiOpsAdminService = aiOpsAdminService;
    }

    @GetMapping
    public ApiResponse<AdminAiConfigViewVO> getConfig() {
        return ApiResponse.success(aiOpsAdminService.getCurrentConfig(), MDC.get("traceId"));
    }

    @PostMapping("/validate")
    public ApiResponse<AiOpsConfigValidationResponse> validate(@RequestBody AdminAiConfigSaveRequest request) {
        return ApiResponse.success(aiOpsAdminService.validate(request), MDC.get("traceId"));
    }

    @PutMapping
    public ApiResponse<AdminAiConfigViewVO> save(@Valid @RequestBody AdminAiConfigSaveRequest request) {
        return ApiResponse.success(aiOpsAdminService.save(request), MDC.get("traceId"));
    }

    @GetMapping("/health")
    public ApiResponse<AiGatewayHealthResponse> health() {
        return ApiResponse.success(aiOpsAdminService.health(), MDC.get("traceId"));
    }

    @GetMapping("/drift")
    public ApiResponse<AdminAiConfigDriftVO> drift() {
        return ApiResponse.success(aiOpsAdminService.getRuntimeDrift(), MDC.get("traceId"));
    }

    @PostMapping("/probes/embedding")
    public ApiResponse<AdminAiEmbeddingProbeVO> probeEmbedding(@Valid @RequestBody AdminAiConfigSaveRequest request) {
        return ApiResponse.success(aiOpsAdminService.probeEmbedding(request), MDC.get("traceId"));
    }

    @PostMapping("/probes/rerank")
    public ApiResponse<AdminAiRerankProbeVO> probeRerank(@Valid @RequestBody AdminAiConfigSaveRequest request) {
        return ApiResponse.success(aiOpsAdminService.probeRerank(request), MDC.get("traceId"));
    }

    @PostMapping("/reindex")
    public ApiResponse<RagReindexResponse> reindex(@Valid @RequestBody RagReindexRequest request) {
        return ApiResponse.success(aiOpsAdminService.triggerReindex(request), MDC.get("traceId"));
    }

    @GetMapping("/reindex-jobs/{jobId}")
    public ApiResponse<RagReindexJobResponse> getReindexJob(@PathVariable("jobId") Long jobId) {
        return ApiResponse.success(aiOpsAdminService.fetchReindexJob(jobId), MDC.get("traceId"));
    }

    @GetMapping("/outbox")
    public ApiResponse<List<AdminOutboxRecordVO>> listOutbox(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ApiResponse.success(aiOpsAdminService.listOutbox(status, limit), MDC.get("traceId"));
    }

    @PostMapping("/runtime/sync")
    public ApiResponse<AdminAiConfigViewVO> syncRuntime(@Valid @RequestBody AdminAiRuntimeSyncRequest request) {
        return ApiResponse.success(aiOpsAdminService.syncRuntime(request), MDC.get("traceId"));
    }

    @PostMapping("/outbox/{id}/replay")
    public ApiResponse<AdminOutboxRecordVO> replayOutbox(@PathVariable("id") Long id) {
        return ApiResponse.success(aiOpsAdminService.replayOutbox(id), MDC.get("traceId"));
    }
}
