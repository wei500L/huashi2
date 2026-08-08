package com.huashi.eftransfer.app.modules.assessment.controller;

import com.huashi.eftransfer.app.modules.assessment.imports.dto.ContentReviewResolutionRequest;
import com.huashi.eftransfer.app.modules.assessment.imports.dto.QuestionBankImportCommitRequest;
import com.huashi.eftransfer.app.modules.assessment.imports.dto.QuestionBankImportPackageRequest;
import com.huashi.eftransfer.app.modules.assessment.imports.service.QuestionBankImportService;
import com.huashi.eftransfer.app.modules.assessment.imports.vo.QuestionBankImportCommitVO;
import com.huashi.eftransfer.app.modules.assessment.imports.vo.QuestionBankImportPreflightVO;
import com.huashi.eftransfer.app.modules.assessment.imports.vo.QuestionBankImportReviewVO;
import com.huashi.eftransfer.app.modules.assessment.imports.vo.QuestionBankItemSummaryVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.page.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Validated
@RestController
@RequestMapping("/api/teacher/assessments")
public class TeacherQuestionBankController {

    private final QuestionBankImportService service;
    private final ObjectMapper objectMapper;

    public TeacherQuestionBankController(QuestionBankImportService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/question-bank")
    public ApiResponse<PageResult<QuestionBankItemSummaryVO>> listItems(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String reviewStatus
    ) {
        return ApiResponse.success(service.listItems(pageNo, pageSize, keyword, tag, reviewStatus), MDC.get("traceId"));
    }

    @GetMapping("/question-bank/import-template.xlsx")
    public ResponseEntity<byte[]> downloadXlsxTemplate() {
        return templateResponse(service.downloadTemplate(false), "lexibridge-question-bank-template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @GetMapping("/question-bank/import-template.json")
    public ResponseEntity<byte[]> downloadJsonTemplate() {
        return templateResponse(service.downloadTemplate(true), "lexibridge-question-bank-template.json", MediaType.APPLICATION_JSON_VALUE);
    }

    @PostMapping(value = "/question-bank/imports/preflight", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<QuestionBankImportPreflightVO> preflight(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(service.preflight(file), MDC.get("traceId"));
    }

    /** Retains the original JSON import contract while the UI uses multipart uploads. */
    @PostMapping(value = "/question-bank/imports/preflight", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<QuestionBankImportPreflightVO> preflightJson(@Valid @RequestBody QuestionBankImportPackageRequest request) throws Exception {
        byte[] bytes = objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8);
        return ApiResponse.success(service.preflight(request, "question-bank-package.json", bytes), MDC.get("traceId"));
    }

    @PostMapping("/question-bank/imports/{importId}/commit")
    public ApiResponse<QuestionBankImportCommitVO> commit(
            @PathVariable Long importId,
            @Valid @RequestBody(required = false) QuestionBankImportCommitRequest request
    ) {
        return ApiResponse.success(service.commit(importId, request == null ? new QuestionBankImportCommitRequest(true) : request), MDC.get("traceId"));
    }

    @PostMapping("/question-bank/imports/{importId}/review-issues/{issueId}")
    public ApiResponse<Void> resolveIssue(
            @PathVariable Long importId,
            @PathVariable Long issueId,
            @Valid @RequestBody ContentReviewResolutionRequest request
    ) {
        service.resolveIssue(importId, issueId, request);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @GetMapping("/question-bank/imports/{importId}/publish-readiness")
    public ApiResponse<QuestionBankImportReviewVO> publishReadiness(@PathVariable Long importId) {
        return ApiResponse.success(service.publishReadiness(importId), MDC.get("traceId"));
    }

    @PostMapping("/question-bank/imports/{importId}/confirm")
    public ApiResponse<QuestionBankImportReviewVO> confirmIssues(
            @PathVariable Long importId,
            @Valid @RequestBody ContentReviewResolutionRequest request
    ) {
        return ApiResponse.success(service.confirmIssues(importId, request.issueIds(), request.resolutionNote()), MDC.get("traceId"));
    }

    @PostMapping("/question-bank/imports/{importId}/approve")
    public ApiResponse<QuestionBankImportCommitVO> approve(@PathVariable Long importId) {
        return ApiResponse.success(service.approveImport(importId), MDC.get("traceId"));
    }

    private ResponseEntity<byte[]> templateResponse(byte[] body, String fileName, String mediaType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(mediaType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(fileName).build());
        headers.setContentLength(body.length);
        return ResponseEntity.ok().headers(headers).body(body);
    }
}
