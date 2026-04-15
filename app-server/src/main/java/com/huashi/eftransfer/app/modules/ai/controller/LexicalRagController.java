package com.huashi.eftransfer.app.modules.ai.controller;

import com.huashi.eftransfer.app.modules.ai.dto.LexicalRagConversationPageQuery;
import com.huashi.eftransfer.app.modules.ai.dto.LexicalRagQueryRequest;
import com.huashi.eftransfer.app.modules.ai.service.LexicalRagQueryService;
import com.huashi.eftransfer.app.modules.ai.vo.LexicalRagAnswerVO;
import com.huashi.eftransfer.app.modules.ai.vo.LexicalRagConversationDetailVO;
import com.huashi.eftransfer.app.modules.ai.vo.LexicalRagConversationSummaryVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.page.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/lexical-rag")
public class LexicalRagController {

    private final LexicalRagQueryService lexicalRagQueryService;

    public LexicalRagController(LexicalRagQueryService lexicalRagQueryService) {
        this.lexicalRagQueryService = lexicalRagQueryService;
    }

    @PostMapping("/query")
    public ApiResponse<LexicalRagAnswerVO> query(@Valid @RequestBody LexicalRagQueryRequest request) {
        return ApiResponse.success(lexicalRagQueryService.query(request), MDC.get("traceId"));
    }

    @GetMapping("/conversations")
    public ApiResponse<PageResult<LexicalRagConversationSummaryVO>> pageConversations(
            @Valid @ModelAttribute LexicalRagConversationPageQuery query
    ) {
        return ApiResponse.success(lexicalRagQueryService.pageConversations(query), MDC.get("traceId"));
    }

    @GetMapping("/conversations/{conversationId}")
    public ApiResponse<LexicalRagConversationDetailVO> conversationDetail(@PathVariable String conversationId) {
        return ApiResponse.success(lexicalRagQueryService.getConversationDetail(conversationId), MDC.get("traceId"));
    }
}
