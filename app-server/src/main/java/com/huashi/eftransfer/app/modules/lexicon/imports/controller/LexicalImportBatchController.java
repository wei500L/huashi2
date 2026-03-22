package com.huashi.eftransfer.app.modules.lexicon.imports.controller;

import com.huashi.eftransfer.app.modules.lexicon.imports.dto.LexicalImportBatchPageQuery;
import com.huashi.eftransfer.app.modules.lexicon.imports.dto.LexicalImportRowPageQuery;
import com.huashi.eftransfer.app.modules.lexicon.imports.dto.LexicalImportRowUpdateRequest;
import com.huashi.eftransfer.app.modules.lexicon.imports.entity.LexicalImportFileEntity;
import com.huashi.eftransfer.app.modules.lexicon.imports.service.LexicalImportBatchService;
import com.huashi.eftransfer.app.modules.lexicon.imports.vo.LexicalImportBatchCreatedVO;
import com.huashi.eftransfer.app.modules.lexicon.imports.vo.LexicalImportBatchDetailVO;
import com.huashi.eftransfer.app.modules.lexicon.imports.vo.LexicalImportBatchSummaryVO;
import com.huashi.eftransfer.app.modules.lexicon.imports.vo.LexicalImportRowVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.page.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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

import java.nio.charset.StandardCharsets;

@Validated
@RestController
@RequestMapping("/api/lexical-pairs/import-batches")
public class LexicalImportBatchController {

    private final LexicalImportBatchService lexicalImportBatchService;

    public LexicalImportBatchController(LexicalImportBatchService lexicalImportBatchService) {
        this.lexicalImportBatchService = lexicalImportBatchService;
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping
    public ApiResponse<LexicalImportBatchCreatedVO> createBatch(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(lexicalImportBatchService.createBatch(file), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping
    public ApiResponse<PageResult<LexicalImportBatchSummaryVO>> pageBatches(@Valid @ModelAttribute LexicalImportBatchPageQuery query) {
        return ApiResponse.success(lexicalImportBatchService.pageBatches(query), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/{batchId}")
    public ApiResponse<LexicalImportBatchDetailVO> getBatchDetail(@PathVariable Long batchId) {
        return ApiResponse.success(lexicalImportBatchService.getBatchDetail(batchId), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/{batchId}/rows")
    public ApiResponse<PageResult<LexicalImportRowVO>> pageRows(
            @PathVariable Long batchId,
            @Valid @ModelAttribute LexicalImportRowPageQuery query
    ) {
        return ApiResponse.success(lexicalImportBatchService.pageRows(batchId, query), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PutMapping("/{batchId}/rows/{rowId}")
    public ApiResponse<LexicalImportRowVO> updateRow(
            @PathVariable Long batchId,
            @PathVariable Long rowId,
            @RequestBody LexicalImportRowUpdateRequest request
    ) {
        return ApiResponse.success(lexicalImportBatchService.updateRow(batchId, rowId, request), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @PostMapping("/{batchId}/commit")
    public ApiResponse<LexicalImportBatchCreatedVO> commitBatch(@PathVariable Long batchId) {
        return ApiResponse.success(lexicalImportBatchService.commitBatch(batchId), MDC.get("traceId"));
    }

    @PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
    @GetMapping("/{batchId}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long batchId) {
        LexicalImportFileEntity file = lexicalImportBatchService.loadFile(batchId);
        MediaType mediaType = file.getContentType() == null || file.getContentType().isBlank()
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(file.getContentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.getOriginalFilename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(file.getFileContent());
    }
}
