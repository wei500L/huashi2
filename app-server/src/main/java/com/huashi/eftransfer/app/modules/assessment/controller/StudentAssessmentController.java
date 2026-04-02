package com.huashi.eftransfer.app.modules.assessment.controller;

import com.huashi.eftransfer.app.modules.assessment.dto.AssessmentHistoryPageQuery;
import com.huashi.eftransfer.app.modules.assessment.dto.SaveAssessmentResponsesRequest;
import com.huashi.eftransfer.app.modules.assessment.service.AssessmentService;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptDetailVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptProgressVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptResultVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptStartVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptSubmitVO;
import com.huashi.eftransfer.app.modules.assessment.vo.StudentAssessmentHistorySummaryVO;
import com.huashi.eftransfer.app.modules.assessment.vo.StudentAssessmentSummaryVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.page.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/student/assessments")
public class StudentAssessmentController {

    private final AssessmentService assessmentService;

    public StudentAssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @GetMapping
    public ApiResponse<List<StudentAssessmentSummaryVO>> listAssessments() {
        return ApiResponse.success(assessmentService.listStudentAssessments(), MDC.get("traceId"));
    }

    @GetMapping("/history")
    public ApiResponse<PageResult<StudentAssessmentHistorySummaryVO>> pageHistory(
            @Valid @ModelAttribute AssessmentHistoryPageQuery query
    ) {
        return ApiResponse.success(assessmentService.pageStudentHistory(query), MDC.get("traceId"));
    }

    @PostMapping("/publishes/{publishId}/start")
    public ApiResponse<AssessmentAttemptStartVO> startOrResumeAttempt(@PathVariable Long publishId) {
        return ApiResponse.success(assessmentService.startOrResumeAttempt(publishId), MDC.get("traceId"));
    }

    @GetMapping("/attempts/{attemptId}")
    public ApiResponse<AssessmentAttemptDetailVO> getAttemptDetail(@PathVariable Long attemptId) {
        return ApiResponse.success(assessmentService.getAttemptDetail(attemptId), MDC.get("traceId"));
    }

    @PostMapping("/attempts/{attemptId}/responses")
    public ApiResponse<AssessmentAttemptProgressVO> saveResponses(
            @PathVariable Long attemptId,
            @Valid @RequestBody SaveAssessmentResponsesRequest request
    ) {
        return ApiResponse.success(assessmentService.saveResponses(attemptId, request), MDC.get("traceId"));
    }

    @PostMapping("/attempts/{attemptId}/submit")
    public ApiResponse<AssessmentAttemptSubmitVO> submitAttempt(@PathVariable Long attemptId) {
        return ApiResponse.success(assessmentService.submitAttempt(attemptId), MDC.get("traceId"));
    }

    @GetMapping("/attempts/{attemptId}/result")
    public ApiResponse<AssessmentAttemptResultVO> getAttemptResult(@PathVariable Long attemptId) {
        return ApiResponse.success(assessmentService.getAttemptResult(attemptId), MDC.get("traceId"));
    }
}
