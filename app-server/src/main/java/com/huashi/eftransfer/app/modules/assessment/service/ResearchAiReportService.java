package com.huashi.eftransfer.app.modules.assessment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.audit.service.AuditLogService;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.assessment.entity.ResearchAggregateSnapshotEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.ResearchAiReportEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.ResearchAggregateSnapshotMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.ResearchAiReportMapper;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentJsonCodec;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchAggregateSnapshotVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchAiReportContentVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchAiReportVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchDimensionStatisticsVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchOptionStatisticsVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchQualityStatisticVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchQuestionStatisticsVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchReactionTimeStatisticsVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.AssessmentAiAnalysisStatus;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResearchAiReportService {

    private final ResearchAccessService accessService;
    private final ResearchAnalyticsService analyticsService;
    private final ResearchAnalyticsProperties properties;
    private final ResearchAggregateSnapshotMapper snapshotMapper;
    private final ResearchAiReportMapper reportMapper;
    private final AssessmentJsonCodec jsonCodec;
    private final AuditLogService auditLogService;

    public ResearchAiReportService(
            ResearchAccessService accessService,
            ResearchAnalyticsService analyticsService,
            ResearchAnalyticsProperties properties,
            ResearchAggregateSnapshotMapper snapshotMapper,
            ResearchAiReportMapper reportMapper,
            AssessmentJsonCodec jsonCodec,
            AuditLogService auditLogService
    ) {
        this.accessService = accessService;
        this.analyticsService = analyticsService;
        this.properties = properties;
        this.snapshotMapper = snapshotMapper;
        this.reportMapper = reportMapper;
        this.jsonCodec = jsonCodec;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public ResearchAiReportVO requestReport(Long publishId, ResearchQueryFilter filter) {
        ResearchAccessService.ResearchPublishAccess access = accessService.requireAccessibleResearchPublish(publishId);
        ResearchAggregateSnapshotEntity snapshot = createOrReuseSnapshot(access, filter);
        if (snapshot.getSampleCount() == null || snapshot.getSampleCount() < properties.getMinSampleSize()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR,
                    "At least " + properties.getMinSampleSize() + " submitted attempts are required before generating a group AI report", 400);
        }
        ResearchAiReportEntity existing = reportMapper.selectOne(Wrappers.<ResearchAiReportEntity>lambdaQuery()
                .eq(ResearchAiReportEntity::getAggregateSnapshotId, snapshot.getId())
                .eq(ResearchAiReportEntity::getPromptVersion, properties.getPromptVersion())
                .last("LIMIT 1"));
        if (existing != null) {
            return toVo(existing, snapshot);
        }
        ResearchAiReportEntity report = new ResearchAiReportEntity();
        report.setPublishId(publishId);
        report.setAggregateSnapshotId(snapshot.getId());
        report.setPromptVersion(properties.getPromptVersion());
        report.setIdempotencyKey(snapshot.getId() + ":" + properties.getPromptVersion());
        report.setStatus(AssessmentAiAnalysisStatus.PENDING.name());
        report.setRetryCount(0);
        report.setRequestedBy(SecurityUtils.getCurrentUserId().orElse(null));
        report.setRequestedAt(LocalDateTime.now());
        try {
            reportMapper.insert(report);
        } catch (DataIntegrityViolationException exception) {
            ResearchAiReportEntity raced = reportMapper.selectOne(Wrappers.<ResearchAiReportEntity>lambdaQuery()
                    .eq(ResearchAiReportEntity::getAggregateSnapshotId, snapshot.getId())
                    .eq(ResearchAiReportEntity::getPromptVersion, properties.getPromptVersion())
                    .last("LIMIT 1"));
            if (raced == null) {
                throw exception;
            }
            return toVo(raced, snapshot);
        }
        auditLogService.record("RESEARCH_AI_REPORT_REQUESTED", "RESEARCH_AI_REPORT", String.valueOf(report.getId()),
                Map.of("publishId", publishId, "snapshotId", snapshot.getId(), "sampleCount", snapshot.getSampleCount()),
                "SUCCESS");
        return toVo(report, snapshot);
    }

    @Transactional(readOnly = true)
    public ResearchAiReportVO latest(Long publishId) {
        accessService.requireAccessibleResearchPublish(publishId);
        ResearchAiReportEntity report = reportMapper.selectOne(Wrappers.<ResearchAiReportEntity>lambdaQuery()
                .eq(ResearchAiReportEntity::getPublishId, publishId)
                .orderByDesc(ResearchAiReportEntity::getRequestedAt)
                .orderByDesc(ResearchAiReportEntity::getId)
                .last("LIMIT 1"));
        if (report == null) {
            return null;
        }
        ResearchAggregateSnapshotEntity snapshot = snapshotMapper.selectById(report.getAggregateSnapshotId());
        auditLogService.record("RESEARCH_AI_REPORT_VIEWED", "RESEARCH_AI_REPORT", String.valueOf(report.getId()),
                Map.of("publishId", publishId), "SUCCESS");
        return toVo(report, snapshot);
    }

    @Transactional(readOnly = true)
    public ResearchAiReportVO getReport(Long reportId) {
        ResearchAiReportEntity report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Research AI report was not found", 404);
        }
        accessService.requireAccessibleResearchPublish(report.getPublishId());
        ResearchAggregateSnapshotEntity snapshot = snapshotMapper.selectById(report.getAggregateSnapshotId());
        auditLogService.record("RESEARCH_AI_REPORT_VIEWED", "RESEARCH_AI_REPORT", String.valueOf(report.getId()),
                Map.of("publishId", report.getPublishId()), "SUCCESS");
        return toVo(report, snapshot);
    }

    @Transactional
    public ResearchAiReportVO retry(Long reportId) {
        ResearchAiReportEntity report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Research AI report was not found", 404);
        }
        accessService.requireAccessibleResearchPublish(report.getPublishId());
        if (!AssessmentAiAnalysisStatus.FAILED.name().equalsIgnoreCase(report.getStatus())
                && !AssessmentAiAnalysisStatus.FALLBACK.name().equalsIgnoreCase(report.getStatus())) {
            return getReport(reportId);
        }
        report.setStatus(AssessmentAiAnalysisStatus.PENDING.name());
        report.setNextRetryAt(null);
        report.setFallbackReason(null);
        reportMapper.updateById(report);
        return getReport(reportId);
    }

    ResearchAggregateSnapshotEntity createOrReuseSnapshot(
            ResearchAccessService.ResearchPublishAccess access,
            ResearchQueryFilter filter
    ) {
        ResearchQuestionStatisticsVO questions = analyticsService.questionStatistics(access.publish().getId(), filter);
        ResearchOptionStatisticsVO options = analyticsService.optionStatistics(access.publish().getId(), filter);
        ResearchDimensionStatisticsVO dimensions = analyticsService.dimensionStatistics(access.publish().getId(), filter);
        ResearchReactionTimeStatisticsVO reactions = analyticsService.reactionTimeStatistics(access.publish().getId(), filter);
        ResearchQualityStatisticVO quality = analyticsService.qualityStatistics(access.publish().getId(), filter);
        Map<String, Object> statistics = new LinkedHashMap<>();
        statistics.put("questions", questions.questions().stream().map(row -> Map.of(
                "questionOrder", row.questionOrder(),
                "questionCode", row.questionCode() == null ? "" : row.questionCode(),
                "questionType", row.questionType(),
                "answeredCount", row.answeredCount(),
                "skippedCount", row.skippedCount(),
                "correctRate", row.correctRate() == null ? "" : row.correctRate(),
                "medianReactionMs", row.medianReactionMs() == null ? "" : row.medianReactionMs()
        )).toList());
        statistics.put("options", options.questions());
        statistics.put("dimensions", dimensions.dimensions());
        statistics.put("reactionTimes", reactions.questions());
        Map<String, Object> qualitySummary = Map.of(
                "validCount", quality.validCount(),
                "flaggedCount", quality.flaggedCount(),
                "flagDistribution", quality.flagDistribution()
        );
        LocalDateTime sourceMax = LocalDateTime.now().withNano(0);
        String filterJson = jsonCodec.write(filter.echo());
        String snapshotKey = sha256(access.publish().getId() + "|" + properties.getStatisticsVersion()
                + "|" + filterJson + "|" + sourceMax + "|" + questions.meta().sampleCount());
        ResearchAggregateSnapshotEntity existing = snapshotMapper.selectOne(Wrappers.<ResearchAggregateSnapshotEntity>lambdaQuery()
                .eq(ResearchAggregateSnapshotEntity::getSnapshotKey, snapshotKey)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        ResearchAggregateSnapshotEntity snapshot = new ResearchAggregateSnapshotEntity();
        snapshot.setPublishId(access.publish().getId());
        snapshot.setPaperId(access.paper().getId());
        snapshot.setSnapshotKey(snapshotKey);
        snapshot.setSnapshotVersion(properties.getStatisticsVersion());
        snapshot.setFilterJson(filterJson);
        snapshot.setSampleCount((int) questions.meta().sampleCount());
        snapshot.setSubmittedCount((int) questions.meta().sampleCount());
        snapshot.setStatisticsJson(jsonCodec.write(statistics));
        snapshot.setQualitySummaryJson(jsonCodec.write(qualitySummary));
        snapshot.setSourceMaxUpdatedAt(sourceMax);
        snapshotMapper.insert(snapshot);
        return snapshot;
    }

    ResearchAiReportVO toVo(ResearchAiReportEntity report, ResearchAggregateSnapshotEntity snapshot) {
        String source = AssessmentAiAnalysisStatus.FALLBACK.name().equalsIgnoreCase(report.getStatus())
                ? "RULE_FALLBACK"
                : AssessmentAiAnalysisStatus.COMPLETED.name().equalsIgnoreCase(report.getStatus()) ? "AI" : null;
        return new ResearchAiReportVO(
                report.getId(),
                report.getPublishId(),
                snapshot == null ? null : new ResearchAggregateSnapshotVO(
                        snapshot.getId(),
                        snapshot.getPublishId(),
                        snapshot.getPaperId(),
                        snapshot.getSnapshotVersion(),
                        snapshot.getSnapshotKey(),
                        snapshot.getSampleCount() == null ? 0 : snapshot.getSampleCount(),
                        snapshot.getSubmittedCount() == null ? 0 : snapshot.getSubmittedCount(),
                        snapshot.getSourceMaxUpdatedAt(),
                        snapshot.getCreatedAt()
                ),
                report.getPromptVersion(),
                report.getStatus(),
                source,
                report.getModelName(),
                snapshot == null ? null : snapshot.getSampleCount(),
                report.getPromptTokens(),
                report.getCompletionTokens(),
                decode(report.getReportJson()),
                decode(report.getRuleFallbackJson()),
                report.getFallbackReason(),
                report.getRequestedAt(),
                report.getCompletedAt()
        );
    }

    private ResearchAiReportContentVO decode(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return jsonCodec.read(json, ResearchAiReportContentVO.class);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
