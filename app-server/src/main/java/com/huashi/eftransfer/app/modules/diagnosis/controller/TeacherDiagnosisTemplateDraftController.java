package com.huashi.eftransfer.app.modules.diagnosis.controller;

import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateDraftPageQuery;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateDraftSaveRequest;
import com.huashi.eftransfer.app.modules.diagnosis.service.DiagnosisTemplateDraftService;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateDraftDetailVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateDraftSummaryVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateDraftValidationResponseVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateDetailVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.page.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
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
@RequestMapping("/api/teacher/diagnosis-template-drafts")
public class TeacherDiagnosisTemplateDraftController {

    private final DiagnosisTemplateDraftService diagnosisTemplateDraftService;

    public TeacherDiagnosisTemplateDraftController(DiagnosisTemplateDraftService diagnosisTemplateDraftService) {
        this.diagnosisTemplateDraftService = diagnosisTemplateDraftService;
    }

    @GetMapping
    public ApiResponse<PageResult<DiagnosisTemplateDraftSummaryVO>> pageQuery(@Valid @ModelAttribute DiagnosisTemplateDraftPageQuery query) {
        return ApiResponse.success(diagnosisTemplateDraftService.pageQuery(query), MDC.get("traceId"));
    }

    @PostMapping
    public ApiResponse<DiagnosisTemplateDraftDetailVO> createBlankDraft() {
        return ApiResponse.success(diagnosisTemplateDraftService.createBlankDraft(), MDC.get("traceId"));
    }

    @PostMapping("/from-template/{templateId}")
    public ApiResponse<DiagnosisTemplateDraftDetailVO> createFromTemplate(@PathVariable Long templateId) {
        return ApiResponse.success(diagnosisTemplateDraftService.createFromTemplate(templateId), MDC.get("traceId"));
    }

    @GetMapping("/{draftId}")
    public ApiResponse<DiagnosisTemplateDraftDetailVO> getDetail(@PathVariable Long draftId) {
        return ApiResponse.success(diagnosisTemplateDraftService.getDetail(draftId), MDC.get("traceId"));
    }

    @PutMapping("/{draftId}")
    public ApiResponse<DiagnosisTemplateDraftDetailVO> save(
            @PathVariable Long draftId,
            @Valid @RequestBody DiagnosisTemplateDraftSaveRequest request
    ) {
        return ApiResponse.success(diagnosisTemplateDraftService.save(draftId, request), MDC.get("traceId"));
    }

    @PostMapping("/{draftId}/validate")
    public ApiResponse<DiagnosisTemplateDraftValidationResponseVO> validate(@PathVariable Long draftId) {
        return ApiResponse.success(diagnosisTemplateDraftService.validate(draftId), MDC.get("traceId"));
    }

    @PostMapping("/{draftId}/publish")
    public ApiResponse<DiagnosisTemplateDetailVO> publish(@PathVariable Long draftId) {
        return ApiResponse.success(diagnosisTemplateDraftService.publish(draftId), MDC.get("traceId"));
    }

    @DeleteMapping("/{draftId}")
    public ApiResponse<Void> delete(@PathVariable Long draftId) {
        diagnosisTemplateDraftService.delete(draftId);
        return ApiResponse.success("Diagnosis template draft deleted", MDC.get("traceId"));
    }
}
