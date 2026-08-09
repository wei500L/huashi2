package com.huashi.eftransfer.app.modules.assessment.controller;

import com.huashi.eftransfer.app.modules.assessment.dto.AssessmentPaperSaveRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.AssessmentPublishRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.ParticipationCodeBatchCreateRequest;
import com.huashi.eftransfer.app.modules.assessment.dto.PublicReleaseUpdateRequest;
import com.huashi.eftransfer.app.modules.assessment.service.AssessmentService;
import com.huashi.eftransfer.app.modules.assessment.service.AssessmentPublicReleaseManagementService;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentPublishDetailVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentPaperDetailVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentPaperSummaryVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentPublishSummaryVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ParticipationCodeBatchCreatedVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ParticipationCodeItemVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ParticipationCodeRevokeResultVO;
import com.huashi.eftransfer.app.modules.assessment.vo.PublicAssessmentReleaseSummaryVO;
import com.huashi.eftransfer.app.modules.assessment.vo.TeacherAssessmentAttemptResultVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.enums.AssessmentPaperPurpose;
import com.huashi.eftransfer.shared.page.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/teacher/assessments")
public class TeacherAssessmentController {

    private final AssessmentService assessmentService;
    private final AssessmentPublicReleaseManagementService publicReleaseManagementService;

    public TeacherAssessmentController(
            AssessmentService assessmentService,
            AssessmentPublicReleaseManagementService publicReleaseManagementService
    ) {
        this.assessmentService = assessmentService;
        this.publicReleaseManagementService = publicReleaseManagementService;
    }

    @GetMapping("/papers")
    public ApiResponse<List<AssessmentPaperSummaryVO>> listPapers(
            @RequestParam(required = false) AssessmentPaperPurpose purpose
    ) {
        return ApiResponse.success(assessmentService.listTeacherPapers(purpose), MDC.get("traceId"));
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

    @GetMapping("/publishes/{publishId}")
    public ApiResponse<AssessmentPublishDetailVO> getPublishDetail(@PathVariable Long publishId) {
        return ApiResponse.success(assessmentService.getPublishDetail(publishId), MDC.get("traceId"));
    }

    @GetMapping("/public-releases")
    public ApiResponse<List<PublicAssessmentReleaseSummaryVO>> listPublicReleases() {
        return ApiResponse.success(publicReleaseManagementService.listReleases(), MDC.get("traceId"));
    }

    @GetMapping("/publishes/{publishId}/participation-codes")
    public ApiResponse<PageResult<ParticipationCodeItemVO>> listParticipationCodes(
            @PathVariable Long publishId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String batchId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(publicReleaseManagementService.listCodes(
                publishId, status, batchId, pageNo, pageSize), MDC.get("traceId"));
    }

    @PostMapping("/publishes/{publishId}/participation-code-batches")
    public ApiResponse<ParticipationCodeBatchCreatedVO> createParticipationCodeBatch(
            @PathVariable Long publishId,
            @Valid @RequestBody ParticipationCodeBatchCreateRequest request
    ) {
        return ApiResponse.success(publicReleaseManagementService.createBatch(publishId, request.count()), MDC.get("traceId"));
    }

    @PostMapping("/publishes/{publishId}/participation-codes/{codeId}/revoke")
    public ApiResponse<ParticipationCodeRevokeResultVO> revokeParticipationCode(
            @PathVariable Long publishId,
            @PathVariable Long codeId
    ) {
        return ApiResponse.success(publicReleaseManagementService.revokeCode(publishId, codeId), MDC.get("traceId"));
    }

    @PostMapping("/publishes/{publishId}/participation-code-batches/{batchId}/revoke-unused")
    public ApiResponse<ParticipationCodeRevokeResultVO> revokeParticipationCodeBatch(
            @PathVariable Long publishId,
            @PathVariable String batchId
    ) {
        return ApiResponse.success(publicReleaseManagementService.revokeBatch(publishId, batchId), MDC.get("traceId"));
    }

    @PatchMapping("/publishes/{publishId}/public-release")
    public ApiResponse<PublicAssessmentReleaseSummaryVO> updatePublicRelease(
            @PathVariable Long publishId,
            @Valid @RequestBody PublicReleaseUpdateRequest request
    ) {
        return ApiResponse.success(publicReleaseManagementService.updateQrEntry(
                publishId, request.qrEntryEnabled()), MDC.get("traceId"));
    }

    @GetMapping("/attempts/{attemptId}/result")
    public ApiResponse<TeacherAssessmentAttemptResultVO> getTeacherAttemptResult(@PathVariable Long attemptId) {
        return ApiResponse.success(assessmentService.getTeacherAttemptResult(attemptId), MDC.get("traceId"));
    }
}
