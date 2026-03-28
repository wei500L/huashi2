package com.huashi.eftransfer.app.modules.lexicon.controller;

import com.huashi.eftransfer.app.modules.lexicon.dto.AddLexicalListItemsRequest;
import com.huashi.eftransfer.app.modules.lexicon.dto.CreateLexicalListRequest;
import com.huashi.eftransfer.app.modules.lexicon.dto.LexicalListItemsPageQuery;
import com.huashi.eftransfer.app.modules.lexicon.dto.LexicalListPageQuery;
import com.huashi.eftransfer.app.modules.lexicon.dto.ReorderLexicalListItemsRequest;
import com.huashi.eftransfer.app.modules.lexicon.dto.UpdateLexicalListRequest;
import com.huashi.eftransfer.app.modules.lexicon.service.LexicalListService;
import com.huashi.eftransfer.app.modules.lexicon.vo.AddLexicalListItemsResultVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.LexicalListDetailVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.LexicalListItemVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.LexicalListSummaryVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.page.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/lexical-lists")
public class LexicalListController {

    private final LexicalListService lexicalListService;

    public LexicalListController(LexicalListService lexicalListService) {
        this.lexicalListService = lexicalListService;
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody CreateLexicalListRequest request) {
        return ApiResponse.success(lexicalListService.create(request), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping
    public ApiResponse<PageResult<LexicalListSummaryVO>> pageQuery(@Valid @ModelAttribute LexicalListPageQuery query) {
        return ApiResponse.success(lexicalListService.pageQuery(query), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/{lexicalListId}")
    public ApiResponse<LexicalListDetailVO> getDetail(@PathVariable Long lexicalListId) {
        return ApiResponse.success(lexicalListService.getDetail(lexicalListId), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PutMapping("/{lexicalListId}")
    public ApiResponse<LexicalListDetailVO> update(
            @PathVariable Long lexicalListId,
            @Valid @RequestBody UpdateLexicalListRequest request
    ) {
        return ApiResponse.success(lexicalListService.update(lexicalListId, request), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @DeleteMapping("/{lexicalListId}")
    public ApiResponse<Void> delete(@PathVariable Long lexicalListId) {
        lexicalListService.delete(lexicalListId);
        return ApiResponse.success("Lexical list deleted", MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/{lexicalListId}/items")
    public ApiResponse<AddLexicalListItemsResultVO> addItems(
            @PathVariable Long lexicalListId,
            @Valid @RequestBody AddLexicalListItemsRequest request
    ) {
        return ApiResponse.success(lexicalListService.addItems(lexicalListId, request), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @DeleteMapping("/{lexicalListId}/items/{itemId}")
    public ApiResponse<Void> deleteItem(@PathVariable Long lexicalListId, @PathVariable Long itemId) {
        lexicalListService.deleteItem(lexicalListId, itemId);
        return ApiResponse.success("Lexical list item deleted", MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/{lexicalListId}/items")
    public ApiResponse<PageResult<LexicalListItemVO>> pageItems(
            @PathVariable Long lexicalListId,
            @Valid @ModelAttribute LexicalListItemsPageQuery query
    ) {
        return ApiResponse.success(lexicalListService.pageItems(lexicalListId, query), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PutMapping("/{lexicalListId}/items/reorder")
    public ApiResponse<LexicalListDetailVO> reorderItems(
            @PathVariable Long lexicalListId,
            @Valid @RequestBody ReorderLexicalListItemsRequest request
    ) {
        return ApiResponse.success(lexicalListService.reorderItems(lexicalListId, request), MDC.get("traceId"));
    }
}
