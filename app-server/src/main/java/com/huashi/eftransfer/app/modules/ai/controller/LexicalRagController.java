package com.huashi.eftransfer.app.modules.ai.controller;

import com.huashi.eftransfer.app.modules.ai.dto.LexicalRagQueryRequest;
import com.huashi.eftransfer.app.modules.ai.service.LexicalRagQueryService;
import com.huashi.eftransfer.app.modules.ai.vo.LexicalRagAnswerVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
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
}
