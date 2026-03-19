package com.huashi.eftransfer.app.modules.analytics.service;

import com.huashi.eftransfer.app.modules.analytics.support.ClassAnalyticsSnapshotPayload;
import com.huashi.eftransfer.app.modules.analytics.support.StudentAnalyticsSnapshotPayload;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsCardVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsErrorDistributionVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsHeatmapCellVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsHeatmapVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsRiskBucketVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsRiskPairVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsScatterPointVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsScatterVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsSeriesVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsTrendVO;
import com.huashi.eftransfer.app.modules.analytics.vo.ClassAnalyticsOverviewVO;
import com.huashi.eftransfer.app.modules.analytics.vo.ClassCompletionByModeVO;
import com.huashi.eftransfer.app.modules.analytics.vo.ClassCompletionRateVO;
import com.huashi.eftransfer.app.modules.analytics.vo.StudentAnalyticsOverviewVO;
import com.huashi.eftransfer.app.modules.analytics.vo.StudentProfileSummaryVO;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Service
public class AnalyticsCsvExportService {

    private static final String[] HEADERS = {
            "recordType",
            "scopeType",
            "scopeId",
            "scopeName",
            "range",
            "bucket",
            "metricKey",
            "metricLabel",
            "label",
            "xKey",
            "yKey",
            "studentUserId",
            "studentName",
            "lexicalPairId",
            "lexicalPairType",
            "englishWord",
            "frenchWord",
            "value",
            "accuracy",
            "avgReactionTimeMs",
            "attemptCount",
            "incorrectCount",
            "riskScore",
            "ratio",
            "classRank",
            "classPercentile",
            "recommendedTrainingMode",
            "primaryRiskLevel"
    };

    private final AnalyticsQueryService analyticsQueryService;

    public AnalyticsCsvExportService(AnalyticsQueryService analyticsQueryService) {
        this.analyticsQueryService = analyticsQueryService;
    }

    public CsvExportFile exportCurrentStudentAnalytics(String range) {
        StudentAnalyticsOverviewVO overview = analyticsQueryService.getCurrentStudentOverview();
        AnalyticsTrendVO trend7d = analyticsQueryService.getCurrentStudentTrends("7d", "day");
        AnalyticsTrendVO trend30d = analyticsQueryService.getCurrentStudentTrends(range, "day");
        AnalyticsHeatmapVO heatmap = analyticsQueryService.getCurrentStudentTransferHeatmap(range, null, null);
        AnalyticsScatterVO scatter = analyticsQueryService.getCurrentStudentScatter(range);
        List<AnalyticsRiskPairVO> highRiskPairs = analyticsQueryService.getCurrentStudentHighRiskPairs(range, 10);
        List<AnalyticsErrorDistributionVO> errorDistribution = analyticsQueryService.getCurrentStudentErrorDistribution(range);

        try (StringWriter writer = new StringWriter();
             CSVPrinter printer = CSVFormat.DEFAULT.builder()
                     .setHeader(HEADERS)
                     .build()
                     .print(writer)) {
            writeStudentOverview(printer, overview, range);
            writeTrend(printer, "TREND_7D", "STUDENT", String.valueOf(overview.studentUserId()), overview.studentName(), trend7d, "7d");
            writeTrend(printer, "TREND_30D", "STUDENT", String.valueOf(overview.studentUserId()), overview.studentName(), trend30d, range);
            writeHeatmap(printer, "HEATMAP", "STUDENT", String.valueOf(overview.studentUserId()), overview.studentName(), heatmap, range);
            writeScatter(printer, "SCATTER", "STUDENT", String.valueOf(overview.studentUserId()), overview.studentName(), scatter, range);
            writeRiskPairs(printer, "TOP_RISK", "STUDENT", String.valueOf(overview.studentUserId()), overview.studentName(), highRiskPairs, range);
            writeErrorDistribution(printer, "ERROR_DISTRIBUTION", "STUDENT", String.valueOf(overview.studentUserId()), overview.studentName(), errorDistribution, range);
            printer.flush();
            return new CsvExportFile(buildFileName("student-analytics", range), writer.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to export student analytics CSV", exception);
        }
    }

    public CsvExportFile exportClassAnalytics(Long classId, String range) {
        ClassAnalyticsOverviewVO overview = analyticsQueryService.getClassOverview(classId, range);
        List<AnalyticsRiskBucketVO> riskDistribution = analyticsQueryService.getClassRiskDistribution(classId);
        AnalyticsHeatmapVO heatmap = analyticsQueryService.getClassTransferHeatmap(classId, range, null, null);
        List<AnalyticsErrorDistributionVO> errorDistribution = analyticsQueryService.getClassErrorDistribution(classId, range);
        ClassCompletionRateVO completionRate = analyticsQueryService.getClassCompletionRate(classId, range, "day");
        List<StudentProfileSummaryVO> students = analyticsQueryService.listStudentProfiles(classId);

        try (StringWriter writer = new StringWriter();
             CSVPrinter printer = CSVFormat.DEFAULT.builder()
                     .setHeader(HEADERS)
                     .build()
                     .print(writer)) {
            writeClassOverview(printer, overview, range);
            writeRiskDistribution(printer, overview, riskDistribution, range);
            writeHeatmap(printer, "HEATMAP", "CLASS", String.valueOf(overview.classId()), overview.className(), heatmap, range);
            writeErrorDistribution(printer, "ERROR_DISTRIBUTION", "CLASS", String.valueOf(overview.classId()), overview.className(), errorDistribution, range);
            writeCompletion(printer, overview, completionRate, range);
            writeStudentSummaries(printer, overview, students, range);
            printer.flush();
            return new CsvExportFile(buildFileName("class-analytics-" + overview.classId(), range), writer.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to export class analytics CSV", exception);
        }
    }

    private void writeStudentOverview(CSVPrinter printer, StudentAnalyticsOverviewVO overview, String range) throws IOException {
        for (AnalyticsCardVO card : overview.cards()) {
            print(printer,
                    "OVERVIEW",
                    "STUDENT",
                    overview.studentUserId(),
                    overview.studentName(),
                    range,
                    null,
                    card.key(),
                    card.label(),
                    null,
                    null,
                    null,
                    overview.studentUserId(),
                    overview.studentName(),
                    null,
                    null,
                    null,
                    null,
                    card.value(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    overview.recommendedTrainingMode(),
                    overview.primaryRiskLevel()
            );
        }

        StudentAnalyticsSnapshotPayload snapshot = overview.latestSnapshot();
        print(printer,
                "SNAPSHOT",
                "STUDENT",
                overview.studentUserId(),
                overview.studentName(),
                range,
                null,
                "latestSnapshot",
                "最新画像",
                null,
                null,
                null,
                overview.studentUserId(),
                overview.studentName(),
                null,
                null,
                null,
                null,
                null,
                snapshot.recentAccuracy(),
                (double) snapshot.recentAvgReactionTimeMs(),
                null,
                null,
                null,
                null,
                null,
                null,
                snapshot.recommendedTrainingMode(),
                snapshot.primaryRiskLevel()
        );
    }

    private void writeClassOverview(CSVPrinter printer, ClassAnalyticsOverviewVO overview, String range) throws IOException {
        for (AnalyticsCardVO card : overview.cards()) {
            print(printer,
                    "OVERVIEW",
                    "CLASS",
                    overview.classId(),
                    overview.className(),
                    range,
                    null,
                    card.key(),
                    card.label(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    card.value(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    overview.latestSnapshot().recommendedFocusModes().isEmpty() ? null : overview.latestSnapshot().recommendedFocusModes().getFirst().mode(),
                    overview.primaryRiskLevel()
            );
        }

        ClassAnalyticsSnapshotPayload snapshot = overview.latestSnapshot();
        print(printer,
                "SNAPSHOT",
                "CLASS",
                overview.classId(),
                overview.className(),
                range,
                null,
                "studentCount",
                "学生数",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                snapshot.studentCount(),
                snapshot.recentAccuracy(),
                (double) snapshot.recentAvgReactionTimeMs(),
                null,
                null,
                null,
                null,
                null,
                null,
                snapshot.recommendedFocusModes().isEmpty() ? null : snapshot.recommendedFocusModes().getFirst().mode(),
                snapshot.primaryRiskLevel()
        );
    }

    private void writeTrend(
            CSVPrinter printer,
            String recordType,
            String scopeType,
            String scopeId,
            String scopeName,
            AnalyticsTrendVO trend,
            String range
    ) throws IOException {
        for (AnalyticsSeriesVO series : trend.series()) {
            for (int index = 0; index < trend.xAxis().size(); index++) {
                print(printer,
                        recordType,
                        scopeType,
                        scopeId,
                        scopeName,
                        range,
                        trend.bucket(),
                        series.key(),
                        series.label(),
                        null,
                        trend.xAxis().get(index),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        series.values().get(index),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                );
            }
        }
    }

    private void writeHeatmap(
            CSVPrinter printer,
            String recordType,
            String scopeType,
            String scopeId,
            String scopeName,
            AnalyticsHeatmapVO heatmap,
            String range
    ) throws IOException {
        for (AnalyticsHeatmapCellVO cell : heatmap.cells()) {
            print(printer,
                    recordType,
                    scopeType,
                    scopeId,
                    scopeName,
                    range,
                    heatmap.meta().bucket(),
                    null,
                    null,
                    null,
                    cell.xKey(),
                    cell.yKey(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    cell.value(),
                    cell.accuracy(),
                    (double) cell.avgReactionTimeMs(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    heatmap.meta().filters().get("trainingMode"),
                    heatmap.meta().filters().get("contextSupportLevel")
            );
        }
    }

    private void writeScatter(
            CSVPrinter printer,
            String recordType,
            String scopeType,
            String scopeId,
            String scopeName,
            AnalyticsScatterVO scatter,
            String range
    ) throws IOException {
        for (AnalyticsScatterPointVO point : scatter.points()) {
            print(printer,
                    recordType,
                    scopeType,
                    scopeId,
                    scopeName,
                    range,
                    null,
                    scatter.x(),
                    scatter.y(),
                    point.label(),
                    null,
                    null,
                    null,
                    null,
                    point.lexicalPairId(),
                    point.lexicalPairType(),
                    null,
                    null,
                    null,
                    point.accuracy(),
                    (double) point.avgReactionTimeMs(),
                    point.attemptCount(),
                    null,
                    point.riskScore(),
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    private void writeRiskPairs(
            CSVPrinter printer,
            String recordType,
            String scopeType,
            String scopeId,
            String scopeName,
            List<AnalyticsRiskPairVO> riskPairs,
            String range
    ) throws IOException {
        for (AnalyticsRiskPairVO pair : riskPairs) {
            print(printer,
                    recordType,
                    scopeType,
                    scopeId,
                    scopeName,
                    range,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    pair.lexicalPairId(),
                    pair.lexicalPairType(),
                    pair.englishWord(),
                    pair.frenchWord(),
                    null,
                    null,
                    null,
                    pair.attemptCount(),
                    pair.incorrectCount(),
                    pair.riskScore(),
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    private void writeErrorDistribution(
            CSVPrinter printer,
            String recordType,
            String scopeType,
            String scopeId,
            String scopeName,
            List<AnalyticsErrorDistributionVO> distributions,
            String range
    ) throws IOException {
        for (AnalyticsErrorDistributionVO item : distributions) {
            print(printer,
                    recordType,
                    scopeType,
                    scopeId,
                    scopeName,
                    range,
                    null,
                    item.key(),
                    item.label(),
                    item.label(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    item.count(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    item.ratio(),
                    null,
                    null,
                    null,
                    null
            );
        }
    }

    private void writeRiskDistribution(
            CSVPrinter printer,
            ClassAnalyticsOverviewVO overview,
            List<AnalyticsRiskBucketVO> buckets,
            String range
    ) throws IOException {
        for (AnalyticsRiskBucketVO bucket : buckets) {
            print(printer,
                    "RISK_DISTRIBUTION",
                    "CLASS",
                    overview.classId(),
                    overview.className(),
                    range,
                    null,
                    "riskBucket",
                    "风险分布",
                    "[" + bucket.bucketStart() + ", " + bucket.bucketEnd() + "]",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    bucket.studentCount(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    overview.primaryRiskLevel()
            );
        }
    }

    private void writeCompletion(
            CSVPrinter printer,
            ClassAnalyticsOverviewVO overview,
            ClassCompletionRateVO completionRate,
            String range
    ) throws IOException {
        print(printer,
                "COMPLETION_OVERVIEW",
                "CLASS",
                overview.classId(),
                overview.className(),
                range,
                null,
                "overallRate",
                "班级训练完成率",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                completionRate.overallRate(),
                null,
                null,
                completionRate.completedStudentCount(),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        writeTrend(printer, "COMPLETION_TREND", "CLASS", String.valueOf(overview.classId()), overview.className(), completionRate.trend(), range);
        for (ClassCompletionByModeVO item : completionRate.byMode()) {
            print(printer,
                    "COMPLETION_BY_MODE",
                    "CLASS",
                    overview.classId(),
                    overview.className(),
                    range,
                    null,
                    item.mode(),
                    "模式完成率",
                    item.mode(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    item.completionRate(),
                    null,
                    null,
                    item.completedStudentCount(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    item.mode(),
                    null
            );
        }
    }

    private void writeStudentSummaries(
            CSVPrinter printer,
            ClassAnalyticsOverviewVO overview,
            List<StudentProfileSummaryVO> students,
            String range
    ) throws IOException {
        for (StudentProfileSummaryVO student : students) {
            print(printer,
                    "STUDENT_SUMMARY",
                    "CLASS",
                    overview.classId(),
                    overview.className(),
                    range,
                    null,
                    "studentSummary",
                    "学生画像摘要",
                    student.studentName(),
                    null,
                    student.studentUserId(),
                    student.studentName(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    student.recentAccuracy(),
                    (double) student.recentAvgReactionTimeMs(),
                    null,
                    null,
                    null,
                    student.recentNegativeTransferRisk(),
                    null,
                    null,
                    student.recommendedTrainingMode(),
                    student.primaryRiskLevel()
            );
        }
    }

    private void print(CSVPrinter printer, Object... values) throws IOException {
        printer.printRecord(values);
    }

    private String buildFileName(String prefix, String range) {
        return prefix + "-" + (range == null || range.isBlank() ? "30d" : range) + "-" + LocalDate.now() + ".csv";
    }

    public record CsvExportFile(
            String filename,
            byte[] content
    ) {
    }
}
