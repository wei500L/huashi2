package com.huashi.eftransfer.ai.modules.internal.controller;

import com.huashi.eftransfer.ai.modules.internal.service.InternalAiService;
import com.huashi.eftransfer.shared.ai.ChatRequest;
import com.huashi.eftransfer.shared.ai.ChatResponse;
import com.huashi.eftransfer.shared.ai.EmbeddingBatchRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import com.huashi.eftransfer.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/ai")
public class InternalAiController {

    private final InternalAiService internalAiService;

    public InternalAiController(InternalAiService internalAiService) {
        this.internalAiService = internalAiService;
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ApiResponse.success(internalAiService.chat(request), MDC.get("traceId"));
    }

    @PostMapping("/chat/structured")
    public ApiResponse<StructuredChatResponse> structuredChat(@Valid @RequestBody StructuredChatRequest request) {
        return ApiResponse.success(internalAiService.structuredChat(request), MDC.get("traceId"));
    }

    @PostMapping("/embed")
    public ApiResponse<EmbeddingResponse> embed(@Valid @RequestBody EmbeddingRequest request) {
        return ApiResponse.success(internalAiService.embed(request), MDC.get("traceId"));
    }

    @PostMapping("/embed/batch")
    public ApiResponse<EmbeddingResponse> embedBatch(@Valid @RequestBody EmbeddingBatchRequest request) {
        return ApiResponse.success(internalAiService.embedBatch(request), MDC.get("traceId"));
    }

    @PostMapping("/rerank")
    public ApiResponse<RerankResponse> rerank(@Valid @RequestBody RerankRequest request) {
        return ApiResponse.success(internalAiService.rerank(request), MDC.get("traceId"));
    }
}
