package com.huashi.eftransfer.app.modules.assessment.controller;

import com.huashi.eftransfer.app.modules.assessment.dto.AssessmentPaperSaveRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.AssessmentPublishRequest;
import com.huashi.eftransfer.app.modules.assessment.service.AssessmentService;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentPaperDetailVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentPaperSummaryVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentPublishSummaryVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/teacher/assessments")
public class TeacherAssessmentController {

    private final AssessmentService assessmentService;

    public TeacherAssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping("/papers")
    public ApiResponse<List<AssessmentPaperSummaryVO>> listPapers() {
        return ApiResponse.success(assessmentService.listTeacherPapers(), MDC.get("traceId"));
    }

    @PostMapping("/papers")
    public ApiResponse<AssessmentPaperDetailVO> createPaper(@Valid @RequestBody AssessmentPaperSaveRequest request) {
        return ApiResponse.success(assessmentService.createPaper(request), MDC.get("traceId"));
    }

    @GetMapping("/papers/{paperId}")
    public ApiResponse<AssessmentPaperDetailVO> getPaperDetail(@PathVariable Long paperId) {
        return ApiResponse.success(assessmentService.getPaperDetail(paperId), MDC.get("traceId"));
    }

    @PutMapping("/papers/{paperId}")
    public ApiResponse<AssessmentPaperDetailVO> updatePaper(
            @PathVariable Long paperId,
            @Valid @RequestBody AssessmentPaperSaveRequest request
    ) {
        return ApiResponse.success(assessmentService.updatePaper(paperId, request), MDC.get("traceId"));
    }

    @PostMapping("/papers/{paperId}/publish")
    public ApiResponse<AssessmentPublishSummaryVO> publishPaper(
            @PathVariable Long paperId,
            @Valid @RequestBody AssessmentPublishRequest request
    ) {
        return ApiResponse.success(assessmentService.publishPaper(paperId, request), MDC.get("traceId"));
    }
}
