package com.huashi.eftransfer.app.modules.training.controller;

import com.huashi.eftransfer.app.modules.training.dto.SaveTrainingProgressRequest;
import com.huashi.eftransfer.app.modules.training.dto.StartTrainingSessionRequest;
import com.huashi.eftransfer.app.modules.training.dto.SubmitTrainingAnswerRequest;
import com.huashi.eftransfer.app.modules.training.dto.TrainingSessionPageQuery;
import com.huashi.eftransfer.app.modules.training.service.TrainingSessionService;
import com.huashi.eftransfer.app.modules.training.vo.TrainingHistorySummaryVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingNextItemVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingSessionCreatedVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingSessionHeartbeatVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingSessionProgressVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingSessionSummaryVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.page.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/training/sessions")
public class TrainingSessionController {

    private final TrainingSessionService trainingSessionService;

    public TrainingSessionController(TrainingSessionService trainingSessionService) {
        this.trainingSessionService = trainingSessionService;
    }

    @PostMapping
    public ApiResponse<TrainingSessionCreatedVO> start(@Valid @RequestBody StartTrainingSessionRequest request) {
        return ApiResponse.success(trainingSessionService.startSession(request), MDC.get("traceId"));
    }

    @GetMapping
    public ApiResponse<PageResult<TrainingHistorySummaryVO>> pageHistory(@Valid TrainingSessionPageQuery query) {
        return ApiResponse.success(trainingSessionService.pageHistory(query), MDC.get("traceId"));
    }

    @GetMapping("/{sessionId}/next-item")
    public ApiResponse<TrainingNextItemVO> getNextItem(@PathVariable Long sessionId) {
        return ApiResponse.success(trainingSessionService.getNextItem(sessionId), MDC.get("traceId"));
    }

    @PostMapping("/{sessionId}/progress")
    public ApiResponse<TrainingSessionProgressVO> saveProgress(
            @PathVariable Long sessionId,
            @Valid @RequestBody SaveTrainingProgressRequest request
    ) {
        return ApiResponse.success(trainingSessionService.saveProgress(sessionId, request), MDC.get("traceId"));
    }

    @PostMapping("/{sessionId}/heartbeat")
    public ApiResponse<TrainingSessionHeartbeatVO> heartbeat(@PathVariable Long sessionId) {
        return ApiResponse.success(trainingSessionService.heartbeatSession(sessionId), MDC.get("traceId"));
    }

    @PostMapping("/{sessionId}/answers")
    public ApiResponse<TrainingSessionProgressVO> submitAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitTrainingAnswerRequest request
    ) {
        return ApiResponse.success(trainingSessionService.submitAnswer(sessionId, request), MDC.get("traceId"));
    }

    @PostMapping("/{sessionId}/complete")
    public ApiResponse<TrainingSessionProgressVO> complete(@PathVariable Long sessionId) {
        return ApiResponse.success(trainingSessionService.completeSession(sessionId), MDC.get("traceId"));
    }

    @PostMapping("/{sessionId}/abandon")
    public ApiResponse<TrainingSessionProgressVO> abandon(@PathVariable Long sessionId) {
        return ApiResponse.success(trainingSessionService.abandonSession(sessionId), MDC.get("traceId"));
    }

    @GetMapping("/{sessionId}/summary")
    public ApiResponse<TrainingSessionSummaryVO> getSummary(@PathVariable Long sessionId) {
        return ApiResponse.success(trainingSessionService.getSummary(sessionId), MDC.get("traceId"));
    }
}
