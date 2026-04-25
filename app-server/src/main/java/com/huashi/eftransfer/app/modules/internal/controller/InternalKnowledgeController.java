package com.huashi.eftransfer.app.modules.internal.controller;

import com.huashi.eftransfer.app.modules.internal.service.InternalKnowledgeService;
import com.huashi.eftransfer.shared.ai.LexicalPairEmbeddingStatusSyncRequest;
import com.huashi.eftransfer.shared.ai.LexicalPairEmbeddingStatusSyncResponse;
import com.huashi.eftransfer.shared.ai.LexicalKnowledgeExportPageResponse;
import com.huashi.eftransfer.shared.api.ApiResponse;
import jakarta.validation.constraints.Min;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@Validated
@RestController
@RequestMapping("/internal/knowledge")
public class InternalKnowledgeController {

    private final InternalKnowledgeService internalKnowledgeService;

    public InternalKnowledgeController(InternalKnowledgeService internalKnowledgeService) {
        this.internalKnowledgeService = internalKnowledgeService;
    }

    @GetMapping("/lexical-pairs/export")
    public ApiResponse<LexicalKnowledgeExportPageResponse> exportLexicalPairs(
            @RequestParam(name = "updatedSince", required = false) OffsetDateTime updatedSince,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "limit", required = false) @Min(value = 1, message = "limit must be greater than 0") Integer limit,
            @RequestParam(name = "ids", required = false) List<Long> ids
    ) {
        return ApiResponse.success(
                internalKnowledgeService.exportLexicalPairs(updatedSince, cursor, limit, ids),
                MDC.get("traceId")
        );
    }

    @PostMapping("/lexical-pairs/embedding-status")
    public ApiResponse<LexicalPairEmbeddingStatusSyncResponse> syncLexicalPairEmbeddingStatus(
            @RequestBody(required = false) LexicalPairEmbeddingStatusSyncRequest request
    ) {
        return ApiResponse.success(
                internalKnowledgeService.syncLexicalPairEmbeddingStatuses(request),
                MDC.get("traceId")
        );
    }
}
