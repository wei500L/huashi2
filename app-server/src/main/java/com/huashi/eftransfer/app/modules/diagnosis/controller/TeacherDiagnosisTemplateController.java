package com.huashi.eftransfer.app.modules.diagnosis.controller;

import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplatePageQuery;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplateUpsertRequest;
import com.huashi.eftransfer.app.modules.diagnosis.service.DiagnosisTemplateService;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateDeleteResultVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateDetailVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateSummaryVO;
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
@RequestMapping("/api/teacher/diagnosis-templates")
public class TeacherDiagnosisTemplateController {

    private final DiagnosisTemplateService diagnosisTemplateService;

    public TeacherDiagnosisTemplateController(DiagnosisTemplateService diagnosisTemplateService) {
        this.diagnosisTemplateService = diagnosisTemplateService;
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody DiagnosisTemplateUpsertRequest request) {
        return ApiResponse.success(diagnosisTemplateService.create(request), MDC.get("traceId"));
    }

    @PutMapping("/{templateId}")
    public ApiResponse<Long> update(@PathVariable Long templateId, @Valid @RequestBody DiagnosisTemplateUpsertRequest request) {
        return ApiResponse.success(diagnosisTemplateService.update(templateId, request), MDC.get("traceId"));
    }

    @GetMapping
    public ApiResponse<PageResult<DiagnosisTemplateSummaryVO>> pageQuery(@Valid @ModelAttribute DiagnosisTemplatePageQuery query) {
        return ApiResponse.success(diagnosisTemplateService.pageQuery(query), MDC.get("traceId"));
    }

    @GetMapping("/{templateId}")
    public ApiResponse<DiagnosisTemplateDetailVO> getDetail(@PathVariable Long templateId) {
        return ApiResponse.success(diagnosisTemplateService.getDetail(templateId), MDC.get("traceId"));
    }

    @DeleteMapping("/{templateId}")
    public ApiResponse<DiagnosisTemplateDeleteResultVO> delete(@PathVariable Long templateId) {
        return ApiResponse.success(diagnosisTemplateService.delete(templateId), MDC.get("traceId"));
    }
}
