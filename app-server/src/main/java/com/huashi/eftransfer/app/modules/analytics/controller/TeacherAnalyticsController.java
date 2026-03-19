package com.huashi.eftransfer.app.modules.analytics.controller;

import com.huashi.eftransfer.app.modules.analytics.service.AnalyticsCsvExportService;
import com.huashi.eftransfer.app.modules.analytics.service.AnalyticsQueryService;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsErrorDistributionVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsHeatmapVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsRiskBucketVO;
import com.huashi.eftransfer.app.modules.analytics.vo.ClassAnalyticsOverviewVO;
import com.huashi.eftransfer.app.modules.analytics.vo.ClassCompletionRateVO;
import com.huashi.eftransfer.app.modules.analytics.vo.StudentProfileSummaryVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherStudentDetailVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeachingClassSummaryVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/teacher/analytics")
public class TeacherAnalyticsController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv");

    private final AnalyticsQueryService analyticsQueryService;
    private final AnalyticsCsvExportService analyticsCsvExportService;

    public TeacherAnalyticsController(
            AnalyticsQueryService analyticsQueryService,
            AnalyticsCsvExportService analyticsCsvExportService
    ) {
        this.analyticsQueryService = analyticsQueryService;
        this.analyticsCsvExportService = analyticsCsvExportService;
    }

    @GetMapping("/classes")
    public ApiResponse<List<TeachingClassSummaryVO>> listClasses() {
        return ApiResponse.success(analyticsQueryService.listAccessibleClasses(), MDC.get("traceId"));
    }

    @GetMapping("/classes/{classId}/overview")
    public ApiResponse<ClassAnalyticsOverviewVO> getClassOverview(
            @PathVariable Long classId,
            @RequestParam(name = "range", defaultValue = "30d") String range
    ) {
        return ApiResponse.success(analyticsQueryService.getClassOverview(classId, range), MDC.get("traceId"));
    }

    @GetMapping("/classes/{classId}/risk-distribution")
    public ApiResponse<List<AnalyticsRiskBucketVO>> getRiskDistribution(@PathVariable Long classId) {
        return ApiResponse.success(analyticsQueryService.getClassRiskDistribution(classId), MDC.get("traceId"));
    }

    @GetMapping("/classes/{classId}/transfer-heatmap")
    public ApiResponse<AnalyticsHeatmapVO> getTransferHeatmap(
            @PathVariable Long classId,
            @RequestParam(name = "range", defaultValue = "30d") String range,
            @RequestParam(name = "trainingMode", required = false) String trainingMode,
            @RequestParam(name = "contextSupportLevel", required = false) String contextSupportLevel
    ) {
        return ApiResponse.success(
                analyticsQueryService.getClassTransferHeatmap(classId, range, trainingMode, contextSupportLevel),
                MDC.get("traceId")
        );
    }

    @GetMapping("/classes/{classId}/error-distribution")
    public ApiResponse<List<AnalyticsErrorDistributionVO>> getErrorDistribution(
            @PathVariable Long classId,
            @RequestParam(name = "range", defaultValue = "30d") String range
    ) {
        return ApiResponse.success(analyticsQueryService.getClassErrorDistribution(classId, range), MDC.get("traceId"));
    }

    @GetMapping("/classes/{classId}/completion-rate")
    public ApiResponse<ClassCompletionRateVO> getCompletionRate(
            @PathVariable Long classId,
            @RequestParam(name = "range", defaultValue = "30d") String range,
            @RequestParam(name = "bucket", defaultValue = "day") String bucket
    ) {
        return ApiResponse.success(analyticsQueryService.getClassCompletionRate(classId, range, bucket), MDC.get("traceId"));
    }

    @GetMapping("/classes/{classId}/students")
    public ApiResponse<List<StudentProfileSummaryVO>> listStudents(@PathVariable Long classId) {
        return ApiResponse.success(analyticsQueryService.listStudentProfiles(classId), MDC.get("traceId"));
    }

    @GetMapping("/classes/{classId}/students/{studentUserId}")
    public ApiResponse<TeacherStudentDetailVO> getStudentDetail(
            @PathVariable Long classId,
            @PathVariable Long studentUserId
    ) {
        return ApiResponse.success(analyticsQueryService.getStudentDetailForTeacher(classId, studentUserId), MDC.get("traceId"));
    }

    @GetMapping(value = "/classes/{classId}/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportClassCsv(
            @PathVariable Long classId,
            @RequestParam(name = "range", defaultValue = "30d") String range
    ) {
        AnalyticsCsvExportService.CsvExportFile file = analyticsCsvExportService.exportClassAnalytics(classId, range);
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(file.content());
    }
}
