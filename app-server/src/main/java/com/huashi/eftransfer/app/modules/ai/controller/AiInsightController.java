package com.huashi.eftransfer.app.modules.ai.controller;

import com.huashi.eftransfer.app.modules.ai.dto.ExplainDiagnosisRequest;
import com.huashi.eftransfer.app.modules.ai.dto.RecommendTrainingRequest;
import com.huashi.eftransfer.app.modules.ai.service.AiInsightService;
import com.huashi.eftransfer.app.modules.ai.vo.AiGuidanceResponseVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/ai")
public class AiInsightController {

    private final AiInsightService aiInsightService;

    public AiInsightController(AiInsightService aiInsightService) {
        this.aiInsightService = aiInsightService;
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
}
