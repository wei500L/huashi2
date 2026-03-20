package com.huashi.eftransfer.app.modules.diagnosis.controller;

import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisTemplatePageQuery;
import com.huashi.eftransfer.app.modules.diagnosis.service.DiagnosisTemplateService;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisTemplateSummaryVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.page.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/student/diagnosis-templates")
public class StudentDiagnosisTemplateController {

    private final DiagnosisTemplateService diagnosisTemplateService;

    public StudentDiagnosisTemplateController(DiagnosisTemplateService diagnosisTemplateService) {
        this.diagnosisTemplateService = diagnosisTemplateService;
    }

    @GetMapping
    public ApiResponse<PageResult<DiagnosisTemplateSummaryVO>> pagePublishedTemplates(
            @Valid @ModelAttribute DiagnosisTemplatePageQuery query
    ) {
        return ApiResponse.success(diagnosisTemplateService.pagePublishedTemplates(query), MDC.get("traceId"));
    }
}
