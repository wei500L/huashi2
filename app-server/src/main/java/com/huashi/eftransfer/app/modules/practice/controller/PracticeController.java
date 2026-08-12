package com.huashi.eftransfer.app.modules.practice.controller;

import com.huashi.eftransfer.app.modules.practice.dto.PracticeSessionPageQuery;
import com.huashi.eftransfer.app.modules.practice.dto.PracticeSpellingCheckRequest;
import com.huashi.eftransfer.app.modules.practice.dto.SavePracticeDraftRequest;
import com.huashi.eftransfer.app.modules.practice.dto.StartPracticeSessionRequest;
import com.huashi.eftransfer.app.modules.practice.dto.SubmitPracticeRequest;
import com.huashi.eftransfer.app.modules.practice.service.PracticeBankService;
import com.huashi.eftransfer.app.modules.practice.service.PracticeSessionService;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeBankVO;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeHistoryVO;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeProgressVO;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeResultVO;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeSessionCreatedVO;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeSessionDetailVO;
import com.huashi.eftransfer.app.modules.practice.vo.PracticeSpellingCheckVO;
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

import java.util.List;

/**
 * Student self-practice endpoints: bank listing, untimed whole-paper sessions,
 * spelling hint checks, grading, results and history. No teacher publish or
 * timer is involved - this is a pure self-testing module.
 */
@Validated
@RestController
@RequestMapping("/api/student/practice")
public class PracticeController {

    private final PracticeBankService practiceBankService;
    private final PracticeSessionService practiceSessionService;

    public PracticeController(
            PracticeBankService practiceBankService,
            PracticeSessionService practiceSessionService
    ) {
        this.practiceBankService = practiceBankService;
        this.practiceSessionService = practiceSessionService;
    }

    @GetMapping("/banks")
    public ApiResponse<List<PracticeBankVO>> listBanks() {
        return ApiResponse.success(practiceBankService.listPracticeBanks(), MDC.get("traceId"));
    }

    @PostMapping("/sessions")
    public ApiResponse<PracticeSessionCreatedVO> start(@Valid @RequestBody StartPracticeSessionRequest request) {
        return ApiResponse.success(practiceSessionService.createSession(request), MDC.get("traceId"));
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<PracticeSessionDetailVO> getDetail(@PathVariable Long sessionId) {
        return ApiResponse.success(practiceSessionService.getDetail(sessionId), MDC.get("traceId"));
    }

    @PostMapping("/sessions/{sessionId}/draft")
    public ApiResponse<PracticeProgressVO> saveDraft(
            @PathVariable Long sessionId,
            @Valid @RequestBody SavePracticeDraftRequest request
    ) {
        return ApiResponse.success(practiceSessionService.saveDraft(sessionId, request), MDC.get("traceId"));
    }

    @PostMapping("/sessions/{sessionId}/answers/spelling-check")
    public ApiResponse<PracticeSpellingCheckVO> checkSpelling(
            @PathVariable Long sessionId,
            @Valid @RequestBody PracticeSpellingCheckRequest request
    ) {
        return ApiResponse.success(practiceSessionService.checkSpelling(sessionId, request), MDC.get("traceId"));
    }

    @PostMapping("/sessions/{sessionId}/complete")
    public ApiResponse<PracticeProgressVO> complete(
            @PathVariable Long sessionId,
            @Valid @RequestBody SubmitPracticeRequest request
    ) {
        return ApiResponse.success(practiceSessionService.complete(sessionId, request), MDC.get("traceId"));
    }

    @PostMapping("/sessions/{sessionId}/abandon")
    public ApiResponse<PracticeProgressVO> abandon(@PathVariable Long sessionId) {
        return ApiResponse.success(practiceSessionService.abandon(sessionId), MDC.get("traceId"));
    }

    @GetMapping("/sessions/{sessionId}/result")
    public ApiResponse<PracticeResultVO> getResult(@PathVariable Long sessionId) {
        return ApiResponse.success(practiceSessionService.getResult(sessionId), MDC.get("traceId"));
    }

    @GetMapping("/history")
    public ApiResponse<PageResult<PracticeHistoryVO>> history(@Valid PracticeSessionPageQuery query) {
        return ApiResponse.success(practiceSessionService.pageHistory(query), MDC.get("traceId"));
    }
}
