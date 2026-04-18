package com.huashi.eftransfer.app.modules.diagnosis.controller;

import com.huashi.eftransfer.app.modules.diagnosis.dto.CreateDiagnosisSessionRequest;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisSessionPageQuery;
import com.huashi.eftransfer.app.modules.diagnosis.dto.SaveDiagnosisProgressRequest;
import com.huashi.eftransfer.app.modules.diagnosis.dto.SubmitDiagnosisAnswerRequest;
import com.huashi.eftransfer.app.modules.diagnosis.service.DiagnosisSessionService;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisHistorySummaryVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisNextItemVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisResultDetailVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisSessionCreatedVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisSessionHeartbeatVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisSessionProgressVO;
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

@Validated
@RestController
@RequestMapping("/api/diagnosis/sessions")
public class DiagnosisSessionController {

    private final DiagnosisSessionService diagnosisSessionService;

    public DiagnosisSessionController(DiagnosisSessionService diagnosisSessionService) {
        this.diagnosisSessionService = diagnosisSessionService;
    }

    @PostMapping
    public ApiResponse<DiagnosisSessionCreatedVO> create(@Valid @RequestBody CreateDiagnosisSessionRequest request) {
        return ApiResponse.success(diagnosisSessionService.createSession(request), MDC.get("traceId"));
    }

    @GetMapping
    public ApiResponse<PageResult<DiagnosisHistorySummaryVO>> pageHistory(@Valid @ModelAttribute DiagnosisSessionPageQuery query) {
        return ApiResponse.success(diagnosisSessionService.pageHistory(query), MDC.get("traceId"));
    }

    @GetMapping("/{sessionId}/next-item")
    public ApiResponse<DiagnosisNextItemVO> getNextItem(@PathVariable Long sessionId) {
        return ApiResponse.success(diagnosisSessionService.getNextItem(sessionId), MDC.get("traceId"));
    }

    @PostMapping("/{sessionId}/answers")
    public ApiResponse<DiagnosisSessionProgressVO> submitAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitDiagnosisAnswerRequest request
    ) {
        return ApiResponse.success(diagnosisSessionService.submitAnswer(sessionId, request), MDC.get("traceId"));
    }

    @PostMapping("/{sessionId}/progress")
    public ApiResponse<DiagnosisSessionProgressVO> saveProgress(
            @PathVariable Long sessionId,
            @Valid @RequestBody SaveDiagnosisProgressRequest request
    ) {
        return ApiResponse.success(diagnosisSessionService.saveProgress(sessionId, request), MDC.get("traceId"));
    }

    @PostMapping("/{sessionId}/heartbeat")
    public ApiResponse<DiagnosisSessionHeartbeatVO> heartbeat(@PathVariable Long sessionId) {
        return ApiResponse.success(diagnosisSessionService.heartbeatSession(sessionId), MDC.get("traceId"));
    }

    @PostMapping("/{sessionId}/complete")
    public ApiResponse<DiagnosisSessionProgressVO> complete(@PathVariable Long sessionId) {
        return ApiResponse.success(diagnosisSessionService.completeSession(sessionId), MDC.get("traceId"));
    }

    @PostMapping("/{sessionId}/abandon")
    public ApiResponse<DiagnosisSessionProgressVO> abandon(@PathVariable Long sessionId) {
        return ApiResponse.success(diagnosisSessionService.abandonSession(sessionId), MDC.get("traceId"));
    }

    @GetMapping("/{sessionId}/result")
    public ApiResponse<DiagnosisResultDetailVO> getResult(@PathVariable Long sessionId) {
        return ApiResponse.success(diagnosisSessionService.getResultDetail(sessionId), MDC.get("traceId"));
    }
}
