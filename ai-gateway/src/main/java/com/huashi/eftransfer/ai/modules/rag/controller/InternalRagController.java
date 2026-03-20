package com.huashi.eftransfer.ai.modules.rag.controller;

import com.huashi.eftransfer.ai.modules.rag.service.KnowledgeIngestionService;
import com.huashi.eftransfer.ai.modules.rag.service.RagService;
import com.huashi.eftransfer.shared.ai.RagAnswerRequest;
import com.huashi.eftransfer.shared.ai.RagAnswerResponse;
import com.huashi.eftransfer.shared.ai.RagExplainRiskRequest;
import com.huashi.eftransfer.shared.ai.RagExplainRiskResponse;
import com.huashi.eftransfer.shared.ai.RagReindexRequest;
import com.huashi.eftransfer.shared.ai.RagReindexResponse;
import com.huashi.eftransfer.shared.ai.RagRetrieveRequest;
import com.huashi.eftransfer.shared.ai.RagRetrieveResponse;
import com.huashi.eftransfer.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai/rag")
public class InternalRagController {

    private final RagService ragService;
    private final KnowledgeIngestionService knowledgeIngestionService;

    public InternalRagController(RagService ragService, KnowledgeIngestionService knowledgeIngestionService) {
        this.ragService = ragService;
        this.knowledgeIngestionService = knowledgeIngestionService;
    }

    @PostMapping("/answer")
    public ApiResponse<RagAnswerResponse> answer(@Valid @RequestBody RagAnswerRequest request) {
        return ApiResponse.success(ragService.answer(request), MDC.get("traceId"));
    }

    @PostMapping("/retrieve")
    public ApiResponse<RagRetrieveResponse> retrieve(@Valid @RequestBody RagRetrieveRequest request) {
        return ApiResponse.success(ragService.retrieve(request), MDC.get("traceId"));
    }

    @PostMapping("/explain-risk")
    public ApiResponse<RagExplainRiskResponse> explainRisk(@Valid @RequestBody RagExplainRiskRequest request) {
        return ApiResponse.success(ragService.explainRisk(request), MDC.get("traceId"));
    }

    @PostMapping("/reindex")
    public ApiResponse<RagReindexResponse> reindex(@Valid @RequestBody RagReindexRequest request) {
        return ApiResponse.success(knowledgeIngestionService.submit(request), MDC.get("traceId"));
    }
}
