package com.huashi.eftransfer.app.modules.assessment.controller;

import com.huashi.eftransfer.app.common.security.ClientRequestContextResolver;
import com.huashi.eftransfer.app.modules.assessment.dto.PublicAssessmentQrEntryRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.PublicAssessmentVerifyRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.PublicAssessmentTimingRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.SpellingAttemptRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.SaveAssessmentResponsesRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.SubmitAssessmentAttemptRequest;
import com.huashi.eftransfer.app.modules.assessment.service.PublicAssessmentService;
import com.huashi.eftransfer.app.modules.assessment.vo.PublicAssessmentAttemptVO;
import com.huashi.eftransfer.app.modules.assessment.vo.PublicAssessmentMetadataVO;
import com.huashi.eftransfer.app.modules.assessment.vo.PublicAssessmentResultVO;
import com.huashi.eftransfer.app.modules.assessment.vo.PublicAssessmentSessionVO;
import com.huashi.eftransfer.app.modules.assessment.vo.SpellingAttemptVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptProgressVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAttemptSubmitVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/public/assessments")
public class PublicAssessmentController {

    public static final String SESSION_COOKIE = "LEXIBRIDGE_SESSION";

    private final PublicAssessmentService publicAssessmentService;

    public PublicAssessmentController(PublicAssessmentService publicAssessmentService) {
        this.publicAssessmentService = publicAssessmentService;
    }

    @GetMapping("/{releaseCode}")
    public ApiResponse<PublicAssessmentMetadataVO> metadata(@PathVariable String releaseCode) {
        return ApiResponse.success(publicAssessmentService.metadata(releaseCode), MDC.get("traceId"));
    }

    @PostMapping("/{releaseCode}/verify")
    public ResponseEntity<ApiResponse<PublicAssessmentSessionVO>> verify(
            @PathVariable String releaseCode,
            @Valid @RequestBody PublicAssessmentVerifyRequest request,
            HttpServletRequest servletRequest
    ) {
        PublicAssessmentService.VerifiedSession verified = publicAssessmentService.verify(
                releaseCode, request, ClientRequestContextResolver.resolveIpAddress(servletRequest));
        return sessionResponse(releaseCode, servletRequest, verified);
    }

    @PostMapping("/{releaseCode}/qr-entry")
    public ResponseEntity<ApiResponse<PublicAssessmentSessionVO>> qrEntry(
            @PathVariable String releaseCode,
            @Valid @RequestBody PublicAssessmentQrEntryRequest request,
            HttpServletRequest servletRequest
    ) {
        PublicAssessmentService.VerifiedSession verified = publicAssessmentService.enterByQr(
                releaseCode, request, ClientRequestContextResolver.resolveIpAddress(servletRequest));
        return sessionResponse(releaseCode, servletRequest, verified);
    }

    private ResponseEntity<ApiResponse<PublicAssessmentSessionVO>> sessionResponse(
            String releaseCode,
            HttpServletRequest servletRequest,
            PublicAssessmentService.VerifiedSession verified
    ) {
        long maxAge = Math.max(0, Duration.between(LocalDateTime.now(), verified.expiresAt()).getSeconds());
        ResponseCookie cookie = ResponseCookie.from(SESSION_COOKIE, verified.token())
                .httpOnly(true)
                .secure(servletRequest.isSecure())
                .sameSite("Lax")
                .path("/api/public/assessments/" + releaseCode)
                .maxAge(maxAge)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(verified.response(), MDC.get("traceId")));
    }

    @GetMapping("/{releaseCode}/attempt")
    public ApiResponse<PublicAssessmentAttemptVO> restore(
            @PathVariable String releaseCode,
            @CookieValue(value = SESSION_COOKIE, required = false) String sessionToken
    ) {
        return ApiResponse.success(publicAssessmentService.restore(releaseCode, sessionToken), MDC.get("traceId"));
    }

    @PostMapping("/{releaseCode}/responses")
    public ApiResponse<AssessmentAttemptProgressVO> saveResponses(
            @PathVariable String releaseCode,
            @CookieValue(value = SESSION_COOKIE, required = false) String sessionToken,
            @Valid @RequestBody SaveAssessmentResponsesRequest request
    ) {
        return ApiResponse.success(publicAssessmentService.saveResponses(releaseCode, sessionToken, request), MDC.get("traceId"));
    }

    @PostMapping("/{releaseCode}/timing")
    public ApiResponse<Void> recordTiming(
            @PathVariable String releaseCode,
            @CookieValue(value = SESSION_COOKIE, required = false) String sessionToken,
            @Valid @RequestBody PublicAssessmentTimingRequest request
    ) {
        publicAssessmentService.recordTiming(releaseCode, sessionToken, request);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @PostMapping("/{releaseCode}/spelling-attempt")
    public ApiResponse<SpellingAttemptVO> attemptSpelling(
            @PathVariable String releaseCode,
            @CookieValue(value = SESSION_COOKIE, required = false) String sessionToken,
            @Valid @RequestBody SpellingAttemptRequest request
    ) {
        return ApiResponse.success(publicAssessmentService.attemptSpelling(releaseCode, sessionToken, request), MDC.get("traceId"));
    }

    @PostMapping("/{releaseCode}/submit")
    public ApiResponse<AssessmentAttemptSubmitVO> submit(
            @PathVariable String releaseCode,
            @CookieValue(value = SESSION_COOKIE, required = false) String sessionToken,
            @Valid @RequestBody SubmitAssessmentAttemptRequest request
    ) {
        return ApiResponse.success(publicAssessmentService.submit(releaseCode, sessionToken, request), MDC.get("traceId"));
    }

    @GetMapping("/{releaseCode}/result")
    public ApiResponse<PublicAssessmentResultVO> result(
            @PathVariable String releaseCode,
            @CookieValue(value = SESSION_COOKIE, required = false) String sessionToken
    ) {
        return ApiResponse.success(publicAssessmentService.result(releaseCode, sessionToken), MDC.get("traceId"));
    }

}
