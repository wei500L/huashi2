package com.huashi.eftransfer.app.modules.assessment.controller;

import com.huashi.eftransfer.app.modules.assessment.dto.ResearchExportRequest;
import com.huashi.eftransfer.app.modules.assessment.service.ResearchAiReportService;
import com.huashi.eftransfer.app.modules.assessment.service.ResearchAnalyticsService;
import com.huashi.eftransfer.app.modules.assessment.service.ResearchExportService;
import com.huashi.eftransfer.app.modules.assessment.service.ResearchFileService;
import com.huashi.eftransfer.app.modules.assessment.service.ResearchQueryFilter;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchAiReportVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchAttemptSummaryVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchDimensionStatisticsVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchExportJobVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchOptionStatisticsVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchPublishOverviewVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchQualityStatisticVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchQuestionStatisticsVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchReactionTimeStatisticsVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchReleaseListItemVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchTextThemeStatisticsVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchAttachmentVO;
import com.huashi.eftransfer.app.modules.assessment.vo.TeacherResearchAttemptDetailVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import com.huashi.eftransfer.shared.page.PageResult;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/teacher/research")
public class TeacherResearchController {

    private final ResearchAnalyticsService analyticsService;
    private final ResearchAiReportService aiReportService;
    private final ResearchExportService exportService;
    private final ResearchFileService fileService;

    public TeacherResearchController(
            ResearchAnalyticsService analyticsService,
            ResearchAiReportService aiReportService,
            ResearchExportService exportService,
            ResearchFileService fileService
    ) {
        this.analyticsService = analyticsService;
        this.aiReportService = aiReportService;
        this.exportService = exportService;
        this.fileService = fileService;
    }

    @GetMapping("/releases")
    public ApiResponse<List<ResearchReleaseListItemVO>> listReleases() {
        return ApiResponse.success(analyticsService.listReleases(), MDC.get("traceId"));
    }

    @GetMapping("/publishes/{publishId}/overview")
    public ApiResponse<ResearchPublishOverviewVO> overview(
            @PathVariable Long publishId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) String qualityFlag,
            @RequestParam(required = false) String aiStatus,
            @RequestParam(required = false) String submittedFrom,
            @RequestParam(required = false) String submittedTo,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(analyticsService.overview(publishId, filter(
                status, entryType, qualityFlag, aiStatus, submittedFrom, submittedTo, keyword)), MDC.get("traceId"));
    }

    @GetMapping("/publishes/{publishId}/attempts")
    public ApiResponse<PageResult<ResearchAttemptSummaryVO>> attempts(
            @PathVariable Long publishId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) String qualityFlag,
            @RequestParam(required = false) String aiStatus,
            @RequestParam(required = false) String submittedFrom,
            @RequestParam(required = false) String submittedTo,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort
    ) {
        return ApiResponse.success(analyticsService.listAttempts(publishId, filter(
                status, entryType, qualityFlag, aiStatus, submittedFrom, submittedTo, keyword), pageNo, pageSize, sort),
                MDC.get("traceId"));
    }

    @GetMapping("/attempts/{attemptId}")
    public ApiResponse<TeacherResearchAttemptDetailVO> attemptDetail(@PathVariable Long attemptId) {
        return ApiResponse.success(analyticsService.getAttemptDetail(attemptId), MDC.get("traceId"));
    }

    @GetMapping("/publishes/{publishId}/statistics/questions")
    public ApiResponse<ResearchQuestionStatisticsVO> questionStats(
            @PathVariable Long publishId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) String qualityFlag,
            @RequestParam(required = false) String aiStatus,
            @RequestParam(required = false) String submittedFrom,
            @RequestParam(required = false) String submittedTo,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(analyticsService.questionStatistics(publishId, filter(
                status, entryType, qualityFlag, aiStatus, submittedFrom, submittedTo, keyword)), MDC.get("traceId"));
    }

    @GetMapping("/publishes/{publishId}/statistics/options")
    public ApiResponse<ResearchOptionStatisticsVO> optionStats(
            @PathVariable Long publishId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) String qualityFlag,
            @RequestParam(required = false) String aiStatus,
            @RequestParam(required = false) String submittedFrom,
            @RequestParam(required = false) String submittedTo,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(analyticsService.optionStatistics(publishId, filter(
                status, entryType, qualityFlag, aiStatus, submittedFrom, submittedTo, keyword)), MDC.get("traceId"));
    }

    @GetMapping("/publishes/{publishId}/statistics/dimensions")
    public ApiResponse<ResearchDimensionStatisticsVO> dimensionStats(
            @PathVariable Long publishId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) String qualityFlag,
            @RequestParam(required = false) String aiStatus,
            @RequestParam(required = false) String submittedFrom,
            @RequestParam(required = false) String submittedTo,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(analyticsService.dimensionStatistics(publishId, filter(
                status, entryType, qualityFlag, aiStatus, submittedFrom, submittedTo, keyword)), MDC.get("traceId"));
    }

    @GetMapping("/publishes/{publishId}/statistics/reaction-times")
    public ApiResponse<ResearchReactionTimeStatisticsVO> reactionStats(
            @PathVariable Long publishId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) String qualityFlag,
            @RequestParam(required = false) String aiStatus,
            @RequestParam(required = false) String submittedFrom,
            @RequestParam(required = false) String submittedTo,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(analyticsService.reactionTimeStatistics(publishId, filter(
                status, entryType, qualityFlag, aiStatus, submittedFrom, submittedTo, keyword)), MDC.get("traceId"));
    }

    @GetMapping("/publishes/{publishId}/statistics/quality")
    public ApiResponse<ResearchQualityStatisticVO> qualityStats(
            @PathVariable Long publishId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) String qualityFlag,
            @RequestParam(required = false) String aiStatus,
            @RequestParam(required = false) String submittedFrom,
            @RequestParam(required = false) String submittedTo,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(analyticsService.qualityStatistics(publishId, filter(
                status, entryType, qualityFlag, aiStatus, submittedFrom, submittedTo, keyword)), MDC.get("traceId"));
    }

    @GetMapping("/publishes/{publishId}/statistics/text-themes")
    public ApiResponse<ResearchTextThemeStatisticsVO> textThemeStats(
            @PathVariable Long publishId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) String qualityFlag,
            @RequestParam(required = false) String aiStatus,
            @RequestParam(required = false) String submittedFrom,
            @RequestParam(required = false) String submittedTo,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(analyticsService.textThemeStatistics(publishId, filter(
                status, entryType, qualityFlag, aiStatus, submittedFrom, submittedTo, keyword)), MDC.get("traceId"));
    }

    @PostMapping("/publishes/{publishId}/exports")
    public ApiResponse<ResearchExportJobVO> createExport(
            @PathVariable Long publishId,
            @Valid @RequestBody ResearchExportRequest request
    ) {
        return ApiResponse.success(exportService.createExport(publishId, request), MDC.get("traceId"));
    }

    @GetMapping("/exports/{jobId}")
    public ApiResponse<ResearchExportJobVO> getExport(@PathVariable Long jobId) {
        return ApiResponse.success(exportService.getJob(jobId), MDC.get("traceId"));
    }

    @GetMapping("/exports/{jobId}/download")
    public ResponseEntity<StreamingResponseBody> downloadExport(@PathVariable Long jobId) {
        return exportService.download(jobId);
    }

    @GetMapping("/files/{fileId}/metadata")
    public ApiResponse<ResearchAttachmentVO> fileMetadata(@PathVariable Long fileId) {
        return ApiResponse.success(fileService.metadata(fileId), MDC.get("traceId"));
    }

    @GetMapping("/files/{fileId}/download")
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable Long fileId) {
        return fileService.download(fileId, false);
    }

    @GetMapping("/files/{fileId}/preview")
    public ResponseEntity<StreamingResponseBody> previewFile(@PathVariable Long fileId) {
        return fileService.download(fileId, true);
    }

    @PostMapping("/publishes/{publishId}/ai-reports")
    public ApiResponse<ResearchAiReportVO> createAiReport(
            @PathVariable Long publishId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) String qualityFlag,
            @RequestParam(required = false) String aiStatus,
            @RequestParam(required = false) String submittedFrom,
            @RequestParam(required = false) String submittedTo,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(aiReportService.requestReport(publishId, filter(
                status, entryType, qualityFlag, aiStatus, submittedFrom, submittedTo, keyword)), MDC.get("traceId"));
    }

    @GetMapping("/publishes/{publishId}/ai-reports/latest")
    public ApiResponse<ResearchAiReportVO> latestAiReport(@PathVariable Long publishId) {
        return ApiResponse.success(aiReportService.latest(publishId), MDC.get("traceId"));
    }

    @GetMapping("/ai-reports/{reportId}")
    public ApiResponse<ResearchAiReportVO> getAiReport(@PathVariable Long reportId) {
        return ApiResponse.success(aiReportService.getReport(reportId), MDC.get("traceId"));
    }

    @PostMapping("/ai-reports/{reportId}/retry")
    public ApiResponse<ResearchAiReportVO> retryAiReport(@PathVariable Long reportId) {
        return ApiResponse.success(aiReportService.retry(reportId), MDC.get("traceId"));
    }

    private ResearchQueryFilter filter(
            String status,
            String entryType,
            String qualityFlag,
            String aiStatus,
            String submittedFrom,
            String submittedTo,
            String keyword
    ) {
        try {
            return ResearchQueryFilter.from(status, entryType, qualityFlag, aiStatus, submittedFrom, submittedTo, keyword);
        } catch (IllegalArgumentException exception) {
            throw new com.huashi.eftransfer.shared.exception.BusinessException(
                    com.huashi.eftransfer.shared.api.ResultCode.VALIDATION_ERROR, exception.getMessage(), 400);
        }
    }
}
