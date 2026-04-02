package com.huashi.eftransfer.app.modules.training.controller;

import com.huashi.eftransfer.app.modules.training.service.TrainingPlanService;
import com.huashi.eftransfer.app.modules.training.vo.RecommendedTrainingPlanVO;
import com.huashi.eftransfer.app.modules.training.vo.ReviewScheduleItemVO;
import com.huashi.eftransfer.app.modules.training.vo.WrongBookItemVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/training")
public class TrainingPlanController {

    private final TrainingPlanService trainingPlanService;

    public TrainingPlanController(TrainingPlanService trainingPlanService) {
        this.trainingPlanService = trainingPlanService;
    }

    @GetMapping("/plans/recommended")
    public ApiResponse<RecommendedTrainingPlanVO> getRecommendedPlan(
            @RequestParam(name = "diagnosisSummaryId", required = false) Long diagnosisSummaryId
    ) {
        return ApiResponse.success(trainingPlanService.getRecommendedPlan(diagnosisSummaryId), MDC.get("traceId"));
    }

    @GetMapping("/wrong-book")
    public ApiResponse<List<WrongBookItemVO>> getWrongBook() {
        return ApiResponse.success(trainingPlanService.getWrongBook(), MDC.get("traceId"));
    }

    @GetMapping("/review-schedule")
    public ApiResponse<List<ReviewScheduleItemVO>> getReviewSchedule(
            @RequestParam(name = "pendingOnly", defaultValue = "true") boolean pendingOnly
    ) {
        return ApiResponse.success(trainingPlanService.getReviewSchedule(pendingOnly), MDC.get("traceId"));
    }
}
