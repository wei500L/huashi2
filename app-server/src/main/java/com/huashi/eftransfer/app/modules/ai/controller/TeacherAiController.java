package com.huashi.eftransfer.app.modules.ai.controller;

import com.huashi.eftransfer.app.modules.ai.dto.TeacherInterventionSuggestRequest;
import com.huashi.eftransfer.app.modules.ai.service.AiInsightService;
import com.huashi.eftransfer.app.modules.ai.vo.AiGuidanceResponseVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/teacher")
public class TeacherAiController {

    private final AiInsightService aiInsightService;

    public TeacherAiController(AiInsightService aiInsightService) {
        this.aiInsightService = aiInsightService;
    }

    @PostMapping("/intervention-suggest")
    public ApiResponse<AiGuidanceResponseVO> interventionSuggest(
            @Valid @RequestBody TeacherInterventionSuggestRequest request
    ) {
        return ApiResponse.success(aiInsightService.suggestTeacherIntervention(request), MDC.get("traceId"));
    }
}
