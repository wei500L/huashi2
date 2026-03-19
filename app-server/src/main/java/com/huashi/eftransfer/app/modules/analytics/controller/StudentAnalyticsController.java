package com.huashi.eftransfer.app.modules.analytics.controller;

import com.huashi.eftransfer.app.modules.analytics.service.AnalyticsCsvExportService;
import com.huashi.eftransfer.app.modules.analytics.service.AnalyticsQueryService;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsErrorDistributionVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsHeatmapVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsRiskPairVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsScatterVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsTrendVO;
import com.huashi.eftransfer.app.modules.analytics.vo.StudentAnalyticsOverviewVO;
import com.huashi.eftransfer.shared.api.ApiResponse;
import org.slf4j.MDC;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/student/analytics")
public class StudentAnalyticsController {

    private static final MediaType TEXT_CSV = new MediaType("text", "csv");

    private final AnalyticsQueryService analyticsQueryService;
    private final AnalyticsCsvExportService analyticsCsvExportService;

    public StudentAnalyticsController(
            AnalyticsQueryService analyticsQueryService,
            AnalyticsCsvExportService analyticsCsvExportService
    ) {
        this.analyticsQueryService = analyticsQueryService;
        this.analyticsCsvExportService = analyticsCsvExportService;
    }

    @GetMapping("/overview")
    public ApiResponse<StudentAnalyticsOverviewVO> getOverview() {
        return ApiResponse.success(analyticsQueryService.getCurrentStudentOverview(), MDC.get("traceId"));
    }

    @GetMapping("/trends")
    public ApiResponse<AnalyticsTrendVO> getTrends(
            @RequestParam(name = "range", defaultValue = "30d") String range,
            @RequestParam(name = "bucket", defaultValue = "day") String bucket
    ) {
        return ApiResponse.success(analyticsQueryService.getCurrentStudentTrends(range, bucket), MDC.get("traceId"));
    }

    @GetMapping("/transfer-heatmap")
    public ApiResponse<AnalyticsHeatmapVO> getTransferHeatmap(
            @RequestParam(name = "range", defaultValue = "30d") String range,
            @RequestParam(name = "trainingMode", required = false) String trainingMode,
            @RequestParam(name = "contextSupportLevel", required = false) String contextSupportLevel
    ) {
        return ApiResponse.success(
                analyticsQueryService.getCurrentStudentTransferHeatmap(range, trainingMode, contextSupportLevel),
                MDC.get("traceId")
        );
    }

    @GetMapping("/scatter")
    public ApiResponse<AnalyticsScatterVO> getScatter(
            @RequestParam(name = "range", defaultValue = "30d") String range
    ) {
        return ApiResponse.success(analyticsQueryService.getCurrentStudentScatter(range), MDC.get("traceId"));
    }

    @GetMapping("/high-risk-pairs")
    public ApiResponse<List<AnalyticsRiskPairVO>> getHighRiskPairs(
            @RequestParam(name = "range", defaultValue = "30d") String range,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        return ApiResponse.success(analyticsQueryService.getCurrentStudentHighRiskPairs(range, limit), MDC.get("traceId"));
    }

    @GetMapping("/error-distribution")
    public ApiResponse<List<AnalyticsErrorDistributionVO>> getErrorDistribution(
            @RequestParam(name = "range", defaultValue = "30d") String range
    ) {
        return ApiResponse.success(analyticsQueryService.getCurrentStudentErrorDistribution(range), MDC.get("traceId"));
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(name = "range", defaultValue = "30d") String range
    ) {
        AnalyticsCsvExportService.CsvExportFile file = analyticsCsvExportService.exportCurrentStudentAnalytics(range);
        return ResponseEntity.ok()
                .contentType(TEXT_CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(file.content());
    }
}
