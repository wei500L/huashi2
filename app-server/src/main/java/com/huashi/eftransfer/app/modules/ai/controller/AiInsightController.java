package com.huashi.eftransfer.app.modules.ai.controller;

import com.huashi.eftransfer.app.modules.ai.dto.ExplainDiagnosisRequest;
import com.huashi.eftransfer.app.modules.ai.dto.PracticeQuestionTutorRequest;
import com.huashi.eftransfer.app.modules.ai.dto.PracticeTutoringRequest;
import com.huashi.eftransfer.app.modules.ai.dto.RecommendTrainingRequest;
import com.huashi.eftransfer.app.modules.ai.service.AiAsyncJobService;
import com.huashi.eftransfer.app.modules.ai.service.AiInsightService;
import com.huashi.eftransfer.app.modules.ai.vo.AiAsyncJobSubmitVO;
import com.huashi.eftransfer.app.modules.ai.vo.AiAsyncJobVO;
import com.huashi.eftransfer.app.modules.ai.vo.AiGuidanceResponseVO;
import com.huashi.eftransfer.app.modules.ai.vo.PracticeQuestionTutorVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/ai")
public class AiInsightController {

    private final AiInsightService aiInsightService;
    private final AiAsyncJobService aiAsyncJobService;

    public AiInsightController(AiInsightService aiInsightService, AiAsyncJobService aiAsyncJobService) {
        this.aiInsightService = aiInsightService;
        this.aiAsyncJobService = aiAsyncJobService;
    }

    @PostMapping("/recommend-training")
    public ApiResponse<AiGuidanceResponseVO> recommendTraining(
            @RequestBody(required = false) RecommendTrainingRequest request
    ) {
        return ApiResponse.success(aiInsightService.recommendTraining(request), MDC.get("traceId"));
    }

    @PostMapping("/explain-diagnosis")
    public ApiResponse<AiGuidanceResponseVO> explainDiagnosis(
            @RequestBody(required = false) ExplainDiagnosisRequest request
    ) {
        return ApiResponse.success(aiInsightService.explainDiagnosis(request), MDC.get("traceId"));
    }

    @PostMapping("/recommend-training/async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<AiAsyncJobSubmitVO> recommendTrainingAsync(
            @RequestBody(required = false) RecommendTrainingRequest request
    ) {
        return ApiResponse.success(aiAsyncJobService.submitRecommendTraining(request), MDC.get("traceId"));
    }

    @PostMapping("/explain-diagnosis/async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<AiAsyncJobSubmitVO> explainDiagnosisAsync(
            @RequestBody(required = false) ExplainDiagnosisRequest request
    ) {
        return ApiResponse.success(aiAsyncJobService.submitExplainDiagnosis(request), MDC.get("traceId"));
    }

    @PostMapping("/practice-tutoring/async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<AiAsyncJobSubmitVO> practiceTutoringAsync(
            @Valid @RequestBody PracticeTutoringRequest request
    ) {
        return ApiResponse.success(aiAsyncJobService.submitPracticeTutoring(request), MDC.get("traceId"));
    }

    @PostMapping("/practice-question-tutor")
    public ApiResponse<PracticeQuestionTutorVO> explainPracticeQuestion(
            @Valid @RequestBody PracticeQuestionTutorRequest request
    ) {
        return ApiResponse.success(aiInsightService.explainPracticeQuestion(request), MDC.get("traceId"));
    }

    @GetMapping("/jobs/{jobId}")
    public ApiResponse<AiAsyncJobVO> getJob(@PathVariable String jobId) {
        return ApiResponse.success(aiAsyncJobService.getJob(jobId), MDC.get("traceId"));
    }
}
