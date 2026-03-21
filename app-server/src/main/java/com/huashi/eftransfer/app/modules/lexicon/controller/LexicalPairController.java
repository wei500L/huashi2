package com.huashi.eftransfer.app.modules.lexicon.controller;

import com.huashi.eftransfer.app.modules.lexicon.dto.LexicalPairPageQuery;
import com.huashi.eftransfer.app.modules.lexicon.dto.LexicalPairUpsertRequest;
import com.huashi.eftransfer.app.modules.lexicon.service.LexicalPairService;
import com.huashi.eftransfer.app.modules.lexicon.vo.CsvImportResultVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.CsvImportTemplateVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.LexicalPairDetailVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.LexicalPairOverviewVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.LexicalPairSummaryVO;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/lexical-pairs")
public class LexicalPairController {

    private final LexicalPairService lexicalPairService;

    public LexicalPairController(LexicalPairService lexicalPairService) {
        this.lexicalPairService = lexicalPairService;
    }

    @GetMapping
    public ApiResponse<PageResult<LexicalPairSummaryVO>> pageQuery(@Valid @ModelAttribute LexicalPairPageQuery query) {
        return ApiResponse.success(lexicalPairService.pageQuery(query), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/overview")
    public ApiResponse<LexicalPairOverviewVO> getOverview() {
        return ApiResponse.success(lexicalPairService.getOverview(), MDC.get("traceId"));
    }

    @GetMapping("/{lexicalPairId}")
    public ApiResponse<LexicalPairDetailVO> getDetail(@PathVariable Long lexicalPairId) {
        return ApiResponse.success(lexicalPairService.getDetail(lexicalPairId), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody LexicalPairUpsertRequest request) {
        return ApiResponse.success(lexicalPairService.create(request), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PutMapping("/{lexicalPairId}")
    public ApiResponse<Long> update(@PathVariable Long lexicalPairId, @Valid @RequestBody LexicalPairUpsertRequest request) {
        return ApiResponse.success(lexicalPairService.update(lexicalPairId, request), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @DeleteMapping("/{lexicalPairId}")
    public ApiResponse<Void> delete(@PathVariable Long lexicalPairId) {
        lexicalPairService.delete(lexicalPairId);
        return ApiResponse.success("Lexical pair deleted", MDC.get("traceId"));
    }

    @GetMapping("/import-template")
    public ApiResponse<CsvImportTemplateVO> getImportTemplate() {
        return ApiResponse.success(lexicalPairService.getImportTemplate(), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/import")
    public ApiResponse<CsvImportResultVO> importCsv(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(lexicalPairService.importCsv(file), MDC.get("traceId"));
    }
}
