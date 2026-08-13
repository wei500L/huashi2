package com.huashi.eftransfer.app.modules.assessment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.audit.service.AuditLogService;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAiAnalysisEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptAnswerEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentAttemptEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentMetricSnapshotEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPaperEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipantEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentParticipationCodeEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPublicReleaseEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentPublishEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentQuestionEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.AssessmentSubmissionFileEntity;
import com.huashi.eftransfer.app.modules.assessment.entity.ResearchAiReportEntity;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAiAnalysisMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAttemptAnswerMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentAttemptMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentMetricSnapshotMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentPaperMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipantMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentParticipationCodeMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentPublicReleaseMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentPublishMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentQuestionMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.AssessmentSubmissionFileMapper;
import com.huashi.eftransfer.app.modules.assessment.mapper.ResearchAiReportMapper;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentJsonCodec;
import com.huashi.eftransfer.app.modules.assessment.support.AssessmentOptionPayload;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentAiAnalysisVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentMetricSnapshotVO;
import com.huashi.eftransfer.app.modules.assessment.vo.AssessmentOptionVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchAiStatusOverviewVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchAttachmentVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchAttemptSummaryVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchDataQualityOverviewVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchDimensionStatisticVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchDimensionStatisticsVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchDistributionStatsVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchFlagCountVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchFunnelVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchOptionStatisticVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchOptionStatisticsVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchPublishOverviewVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchQualityStatisticVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchQuestionStatisticVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchQuestionStatisticsVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchRateVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchRatesVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchReactionTimeStatisticVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchReactionTimeStatisticsVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchReleaseListItemVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchStatisticsMetaVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchTextThemeStatisticVO;
import com.huashi.eftransfer.app.modules.assessment.vo.ResearchTextThemeStatisticsVO;
import com.huashi.eftransfer.app.modules.assessment.vo.TeacherResearchAttemptDetailVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.AssessmentFileBindingStatus;
import com.huashi.eftransfer.shared.enums.AssessmentFileScanStatus;
import com.huashi.eftransfer.shared.enums.AssessmentPaperPurpose;
import com.huashi.eftransfer.shared.enums.AssessmentQuestionType;
import com.huashi.eftransfer.shared.enums.AssessmentSubmitReason;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResearchAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(ResearchAnalyticsService.class);
    public static final String METRIC_VERSION = "RESEARCH_STATS_V1";

    private final ResearchAccessService accessService;
    private final ResearchAnalyticsProperties properties;
    private final AssessmentPublishMapper publishMapper;
    private final AssessmentPaperMapper paperMapper;
    private final AssessmentPublicReleaseMapper publicReleaseMapper;
    private final AssessmentParticipationCodeMapper participationCodeMapper;
    private final AssessmentParticipantMapper participantMapper;
    private final AssessmentAttemptMapper attemptMapper;
    private final AssessmentAttemptAnswerMapper answerMapper;
    private final AssessmentQuestionMapper questionMapper;
    private final AssessmentMetricSnapshotMapper metricSnapshotMapper;
    private final AssessmentAiAnalysisMapper aiAnalysisMapper;
    private final AssessmentSubmissionFileMapper fileMapper;
    private final ResearchAiReportMapper reportMapper;
    private final AssessmentJsonCodec jsonCodec;
    private final AuditLogService auditLogService;

    public ResearchAnalyticsService(
            ResearchAccessService accessService,
            ResearchAnalyticsProperties properties,
            AssessmentPublishMapper publishMapper,
            AssessmentPaperMapper paperMapper,
            AssessmentPublicReleaseMapper publicReleaseMapper,
            AssessmentParticipationCodeMapper participationCodeMapper,
            AssessmentParticipantMapper participantMapper,
            AssessmentAttemptMapper attemptMapper,
            AssessmentAttemptAnswerMapper answerMapper,
            AssessmentQuestionMapper questionMapper,
            AssessmentMetricSnapshotMapper metricSnapshotMapper,
            AssessmentAiAnalysisMapper aiAnalysisMapper,
            AssessmentSubmissionFileMapper fileMapper,
            ResearchAiReportMapper reportMapper,
            AssessmentJsonCodec jsonCodec,
            AuditLogService auditLogService
    ) {
        this.accessService = accessService;
        this.properties = properties;
        this.publishMapper = publishMapper;
        this.paperMapper = paperMapper;
        this.publicReleaseMapper = publicReleaseMapper;
        this.participationCodeMapper = participationCodeMapper;
        this.participantMapper = participantMapper;
        this.attemptMapper = attemptMapper;
        this.answerMapper = answerMapper;
        this.questionMapper = questionMapper;
        this.metricSnapshotMapper = metricSnapshotMapper;
        this.aiAnalysisMapper = aiAnalysisMapper;
        this.fileMapper = fileMapper;
        this.reportMapper = reportMapper;
        this.jsonCodec = jsonCodec;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ResearchReleaseListItemVO> listReleases() {
        Long currentUserId = com.huashi.eftransfer.app.common.util.SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
        boolean admin = com.huashi.eftransfer.app.common.util.SecurityUtils.getCurrentPrincipal()
                .map(principal -> principal.roles().contains("ADMIN"))
                .orElse(false);
        List<AssessmentPaperEntity> papers = paperMapper.selectList(Wrappers.<AssessmentPaperEntity>lambdaQuery()
                .eq(AssessmentPaperEntity::getPaperPurpose, AssessmentPaperPurpose.RESEARCH_SURVEY.name())
                .eq(!admin, AssessmentPaperEntity::getOwnerUserId, currentUserId)
                .orderByDesc(AssessmentPaperEntity::getUpdatedAt));
        if (papers.isEmpty()) {
            return List.of();
        }
        Map<Long, AssessmentPaperEntity> paperById = papers.stream()
                .collect(Collectors.toMap(AssessmentPaperEntity::getId, Function.identity(), (left, right) -> left));
        List<AssessmentPublishEntity> publishes = publishMapper.selectList(Wrappers.<AssessmentPublishEntity>lambdaQuery()
                .in(AssessmentPublishEntity::getPaperId, paperById.keySet())
                .eq(AssessmentPublishEntity::getDeliveryMode, "PUBLIC_CODE")
                .orderByDesc(AssessmentPublishEntity::getPublishedAt)
                .orderByDesc(AssessmentPublishEntity::getId));
        if (publishes.isEmpty()) {
            return List.of();
        }
        List<Long> publishIds = publishes.stream().map(AssessmentPublishEntity::getId).toList();
        Map<Long, AssessmentPublicReleaseEntity> releases = publicReleaseMapper.selectList(
                        Wrappers.<AssessmentPublicReleaseEntity>lambdaQuery()
                                .in(AssessmentPublicReleaseEntity::getPublishId, publishIds))
                .stream()
                .collect(Collectors.toMap(AssessmentPublicReleaseEntity::getPublishId, Function.identity(), (left, right) -> left));
        Map<Long, List<AssessmentAttemptEntity>> attemptsByPublish = loadAttempts(publishIds).stream()
                .collect(Collectors.groupingBy(AssessmentAttemptEntity::getPublishId));
        Map<Long, String> latestReportStatus = latestReportStatus(publishIds);
        List<ResearchReleaseListItemVO> items = new ArrayList<>();
        for (AssessmentPublishEntity publish : publishes) {
            AssessmentPaperEntity paper = paperById.get(publish.getPaperId());
            AssessmentPublicReleaseEntity release = releases.get(publish.getId());
            List<AssessmentAttemptEntity> attempts = attemptsByPublish.getOrDefault(publish.getId(), List.of());
            long started = attempts.size();
            long submitted = attempts.stream().filter(this::isSubmitted).count();
            LocalDateTime latestSubmission = attempts.stream()
                    .map(AssessmentAttemptEntity::getSubmittedAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
            items.add(new ResearchReleaseListItemVO(
                    publish.getId(),
                    publish.getPaperId(),
                    paper == null ? publish.getPaperTitleSnapshot() : paper.getTitle(),
                    release == null ? null : release.getReleaseCode(),
                    publish.getPublishedAt(),
                    publish.getStatus(),
                    (int) started,
                    (int) submitted,
                    latestSubmission,
                    latestReportStatus.get(publish.getId())
            ));
        }
        return items;
    }

    @Transactional(readOnly = true)
    public ResearchPublishOverviewVO overview(Long publishId, ResearchQueryFilter filter) {
        ResearchAccessService.ResearchPublishAccess access = accessService.requireAccessibleResearchPublish(publishId);
        Dataset dataset = loadDataset(access, filter);
        LocalDateTime now = LocalDateTime.now();
        List<AssessmentParticipationCodeEntity> codes = participationCodeMapper.selectList(
                Wrappers.<AssessmentParticipationCodeEntity>lambdaQuery()
                        .eq(AssessmentParticipationCodeEntity::getPublicReleaseId, access.release().getId()));
        long codeGenerated = codes.stream().filter(code -> !"REVOKED".equalsIgnoreCase(code.getStatus())).count();
        long codeVerified = codes.stream().filter(code -> code.getFirstVerifiedAt() != null).count();
        long participantCreated = dataset.allParticipants.size();
        long attemptStarted = dataset.allAttempts.size();
        long inProgress = dataset.allAttempts.stream().filter(attempt -> !isSubmitted(attempt)).count();
        long submitted = dataset.allAttempts.stream().filter(this::isSubmitted).count();
        long expired = dataset.allAttempts.stream().filter(this::isExpired).count();
        if (AssessmentPublicReleaseManagementService.resolveMaxAttempts(
                access.release() == null ? null : access.release().getMaxAttempts()) > 1) {
            attemptStarted = uniqueParticipantCount(dataset.allAttempts);
            inProgress = uniqueParticipantCount(dataset.allAttempts.stream().filter(attempt -> !isSubmitted(attempt)).toList());
            submitted = uniqueParticipantCount(dataset.allAttempts.stream().filter(this::isSubmitted).toList());
            expired = uniqueParticipantCount(dataset.allAttempts.stream().filter(this::isExpired).toList());
        }
        ResearchFunnelVO funnel = new ResearchFunnelVO(
                codeGenerated, codeVerified, participantCreated, attemptStarted, inProgress, submitted, expired);
        ResearchRatesVO rates = new ResearchRatesVO(
                ResearchRateVO.of(submitted, attemptStarted),
                ResearchRateVO.of(codeVerified, codeGenerated),
                ResearchRateVO.of(submitted, participantCreated)
        );
        List<Long> completionMs = dataset.statsAttempts.stream()
                .filter(this::isSubmitted)
                .map(this::effectiveDuration)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        List<Double> scores = dataset.statsAttempts.stream()
                .map(attempt -> dataset.metricsByAttempt.get(attempt.getId()))
                .filter(Objects::nonNull)
                .map(AssessmentMetricSnapshotEntity::getPercentageScore)
                .filter(Objects::nonNull)
                .map(java.math.BigDecimal::doubleValue)
                .sorted()
                .toList();
        ResearchDistributionStatsVO timing = distributionFromLongs(completionMs);
        ResearchDistributionStatsVO score = distributionFromDoubles(scores);
        ResearchDataQualityOverviewVO quality = qualityOverview(dataset);
        ResearchAiStatusOverviewVO ai = aiOverview(dataset);
        LocalDateTime latestSubmission = dataset.allAttempts.stream()
                .map(AssessmentAttemptEntity::getSubmittedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        return new ResearchPublishOverviewVO(
                access.publish().getId(),
                access.paper().getId(),
                access.paper().getTitle(),
                access.release().getReleaseCode(),
                funnel,
                rates,
                timing,
                score,
                quality,
                ai,
                latestSubmission,
                now
        );
    }

    @Transactional(readOnly = true)
    public PageResult<ResearchAttemptSummaryVO> listAttempts(
            Long publishId,
            ResearchQueryFilter filter,
            int pageNo,
            int pageSize,
            String sort
    ) {
        ResearchAccessService.ResearchPublishAccess access = accessService.requireAccessibleResearchPublish(publishId);
        Dataset dataset = loadDataset(access, filter);
        List<ResearchAttemptSummaryVO> rows = dataset.filteredAttempts.stream()
                .map(attempt -> toAttemptSummary(attempt, dataset))
                .toList();
        List<ResearchAttemptSummaryVO> sorted = sortAttempts(rows, sort);
        int safePageNo = Math.max(1, pageNo);
        int safePageSize = Math.min(100, Math.max(1, pageSize));
        int from = Math.min((safePageNo - 1) * safePageSize, sorted.size());
        int to = Math.min(from + safePageSize, sorted.size());
        return new PageResult<>(sorted.size(), safePageNo, safePageSize, sorted.subList(from, to));
    }

    @Transactional(readOnly = true)
    public TeacherResearchAttemptDetailVO getAttemptDetail(Long attemptId) {
        ResearchAccessService.ResearchAttemptAccess access = accessService.requireAccessibleResearchAttempt(attemptId);
        AssessmentAttemptEntity attempt = access.attempt();
        AssessmentParticipantEntity participant = attempt.getParticipantId() == null
                ? null
                : participantMapper.selectById(attempt.getParticipantId());
        Dataset dataset = loadDataset(access.publishAccess(), ResearchQueryFilter.from(null, null, null, null, null, null, null));
        Map<Long, AssessmentQuestionEntity> questions = loadQuestions(attempt.getPaperId());
        List<AssessmentAttemptAnswerEntity> answers = answerMapper.selectList(Wrappers.<AssessmentAttemptAnswerEntity>lambdaQuery()
                .eq(AssessmentAttemptAnswerEntity::getAttemptId, attempt.getId())
                .orderByAsc(AssessmentAttemptAnswerEntity::getQuestionOrder)
                .orderByAsc(AssessmentAttemptAnswerEntity::getId));
        List<AssessmentSubmissionFileEntity> files = dataset.filesByAttempt.getOrDefault(attempt.getId(), List.of());
        Map<Long, List<AssessmentSubmissionFileEntity>> filesByQuestion = files.stream()
                .collect(Collectors.groupingBy(AssessmentSubmissionFileEntity::getQuestionId));
        AssessmentMetricSnapshotEntity metric = dataset.metricsByAttempt.get(attempt.getId());
        AssessmentAiAnalysisEntity analysis = dataset.analysesByAttempt.get(attempt.getId());
        try {
            auditLogService.record("RESEARCH_RESPONSE_VIEWED", "ASSESSMENT_ATTEMPT", String.valueOf(attempt.getId()),
                    Map.of("publishId", attempt.getPublishId()), "SUCCESS");
        } catch (RuntimeException exception) {
            log.warn("event=research_response_view_audit_failed attemptId={} reason={}",
                    attempt.getId(), exception.getMessage());
        }
        List<TeacherResearchAttemptDetailVO.ResearchAttemptQuestionVO> questionVos = answers.stream()
                .map(answer -> {
                    AssessmentQuestionEntity question = questions.get(answer.getQuestionId());
                    List<AssessmentOptionVO> options = jsonCodec.readOptions(answer.getOptionsJsonSnapshot()).stream()
                            .map(option -> new AssessmentOptionVO(option.key(), option.label()))
                            .toList();
                    return new TeacherResearchAttemptDetailVO.ResearchAttemptQuestionVO(
                            answer.getQuestionId(),
                            answer.getQuestionOrder(),
                            answer.getQuestionType(),
                            question == null ? null : question.getTargetWord(),
                            question == null ? null : question.getSectionCode(),
                            resolveFormalSection(question, answer.getQuestionType()),
                            answer.getStemTextSnapshot(),
                            answer.getPromptTextSnapshot(),
                            options,
                            jsonCodec.readStringList(answer.getResponseJson()),
                            jsonCodec.readStringList(answer.getCorrectAnswerJson()),
                            answer.getJustificationText(),
                            answer.getCorrect(),
                            answer.getScoreAwarded(),
                            answer.getQuestionScore(),
                            answer.getExplanationTextSnapshot(),
                            answer.getEffectiveDurationMs(),
                            answer.getResponseChangeCount(),
                            filesByQuestion.getOrDefault(answer.getQuestionId(), List.of()).stream()
                                    .map(this::toAttachment)
                                    .toList()
                    );
                })
                .toList();
        return new TeacherResearchAttemptDetailVO(
                new TeacherResearchAttemptDetailVO.ResearchAttemptParticipantVO(
                        formatParticipantCode(attempt.getParticipantId()),
                        participant == null ? null : participant.getParticipantType(),
                        participant == null ? null : participant.getConsentedAt()
                ),
                new TeacherResearchAttemptDetailVO.ResearchAttemptStateVO(
                        attempt.getId(),
                        PublicAssessmentService.attemptNoOf(attempt),
                        attempt.getPublishId(),
                        attempt.getPaperId(),
                        access.publishAccess().paper().getTitle(),
                        attempt.getStatus(),
                        attempt.getAnsweredCount(),
                        access.publishAccess().publish().getQuestionCountSnapshot(),
                        attempt.getStartedAt(),
                        attempt.getLastSavedAt(),
                        attempt.getSubmittedAt(),
                        attempt.getSubmitReason()
                ),
                new TeacherResearchAttemptDetailVO.ResearchAttemptResultSummaryVO(
                        attempt.getObjectiveScore(),
                        attempt.getTotalScore(),
                        metric == null || metric.getPercentageScore() == null ? null : metric.getPercentageScore().doubleValue(),
                        metric == null ? null : jsonCodec.read(metric.getMetricsJson(), AssessmentMetricSnapshotVO.class),
                        metric == null ? List.of() : jsonCodec.readStringList(metric.getQualityFlagsJson())
                ),
                new TeacherResearchAttemptDetailVO.ResearchAttemptAiVO(
                        analysis == null ? null : analysis.getStatus(),
                        analysis == null ? null : decodeAnalysis(analysis),
                        analysis == null ? null : analysis.getModelName(),
                        analysis == null ? null : analysis.getCompletedAt(),
                        analysis == null ? null : analysis.getFallbackReason()
                ),
                questionVos
        );
    }

    @Transactional(readOnly = true)
    public ResearchQuestionStatisticsVO questionStatistics(Long publishId, ResearchQueryFilter filter) {
        Dataset dataset = loadFilteredDataset(publishId, filter);
        List<ResearchQuestionStatisticVO> rows = new ArrayList<>();
        for (AssessmentQuestionEntity question : dataset.questions) {
            QuestionAgg agg = dataset.questionAgg.getOrDefault(question.getId(), QuestionAgg.empty());
            Double correctRate = isAutoScored(question.getQuestionType()) && agg.validAnswered > 0
                    ? agg.correct / (double) agg.validAnswered
                    : null;
            rows.add(new ResearchQuestionStatisticVO(
                    question.getId(),
                    question.getSortOrder(),
                    questionCode(question),
                    question.getSectionCode(),
                    question.getQuestionType(),
                    agg.answered,
                    agg.skipped,
                    correctRate,
                    percentile(agg.reactionTimes, 50),
                    agg.fastItem
            ));
        }
        return new ResearchQuestionStatisticsVO(meta(dataset), rows);
    }

    @Transactional(readOnly = true)
    public ResearchOptionStatisticsVO optionStatistics(Long publishId, ResearchQueryFilter filter) {
        Dataset dataset = loadFilteredDataset(publishId, filter);
        long submitted = dataset.submittedAttemptIds.size();
        List<ResearchOptionStatisticVO> rows = new ArrayList<>();
        for (AssessmentQuestionEntity question : dataset.questions) {
            if (!isChoiceType(question.getQuestionType())) {
                continue;
            }
            QuestionAgg agg = dataset.questionAgg.getOrDefault(question.getId(), QuestionAgg.empty());
            List<AssessmentOptionPayload> options = jsonCodec.readOptions(question.getOptionsJson());
            List<ResearchOptionStatisticVO.ResearchOptionShareVO> shares = new ArrayList<>();
            for (AssessmentOptionPayload option : options) {
                long count = agg.optionCounts.getOrDefault(option.key().toUpperCase(Locale.ROOT), 0L);
                shares.add(new ResearchOptionStatisticVO.ResearchOptionShareVO(
                        option.key(),
                        option.label(),
                        count,
                        agg.answered == 0 ? null : count / (double) agg.answered,
                        submitted == 0 ? null : count / (double) submitted
                ));
            }
            Double exactCorrectRate = agg.validAnswered == 0 ? null : agg.correct / (double) agg.validAnswered;
            rows.add(new ResearchOptionStatisticVO(
                    question.getId(),
                    question.getSortOrder(),
                    questionCode(question),
                    question.getQuestionType(),
                    exactCorrectRate,
                    shares
            ));
        }
        return new ResearchOptionStatisticsVO(meta(dataset), rows);
    }

    @Transactional(readOnly = true)
    public ResearchDimensionStatisticsVO dimensionStatistics(Long publishId, ResearchQueryFilter filter) {
        Dataset dataset = loadFilteredDataset(publishId, filter);
        Map<String, long[]> counts = new LinkedHashMap<>();
        for (AssessmentQuestionEntity question : dataset.questions) {
            if (!isAutoScored(question.getQuestionType())) {
                continue;
            }
            String dimension = firstNonBlank(question.getConstructCode(), question.getTransferCategory(), question.getSectionCode());
            if (dimension == null) {
                continue;
            }
            QuestionAgg agg = dataset.questionAgg.getOrDefault(question.getId(), QuestionAgg.empty());
            long[] pair = counts.computeIfAbsent(dimension, key -> new long[2]);
            pair[0] += agg.validAnswered;
            pair[1] += agg.correct;
        }
        List<ResearchDimensionStatisticVO> rows = counts.entrySet().stream()
                .map(entry -> new ResearchDimensionStatisticVO(
                        entry.getKey(),
                        entry.getValue()[0],
                        entry.getValue()[1],
                        entry.getValue()[0] == 0 ? null : entry.getValue()[1] / (double) entry.getValue()[0]
                ))
                .toList();
        return new ResearchDimensionStatisticsVO(meta(dataset), rows);
    }

    @Transactional(readOnly = true)
    public ResearchReactionTimeStatisticsVO reactionTimeStatistics(Long publishId, ResearchQueryFilter filter) {
        Dataset dataset = loadFilteredDataset(publishId, filter);
        List<ResearchReactionTimeStatisticVO> rows = dataset.questions.stream()
                .map(question -> {
                    QuestionAgg agg = dataset.questionAgg.getOrDefault(question.getId(), QuestionAgg.empty());
                    List<Long> times = agg.reactionTimes.stream().sorted().toList();
                    return new ResearchReactionTimeStatisticVO(
                            question.getId(),
                            question.getSortOrder(),
                            questionCode(question),
                            times.size(),
                            percentile(times, 50),
                            percentile(times, 25),
                            percentile(times, 75),
                            percentile(times, 90)
                    );
                })
                .toList();
        return new ResearchReactionTimeStatisticsVO(meta(dataset), rows);
    }

    @Transactional(readOnly = true)
    public ResearchQualityStatisticVO qualityStatistics(Long publishId, ResearchQueryFilter filter) {
        Dataset dataset = loadFilteredDataset(publishId, filter);
        ResearchDataQualityOverviewVO overview = qualityOverview(dataset);
        return new ResearchQualityStatisticVO(meta(dataset), overview.valid(), overview.flagged(), overview.flagDistribution());
    }

    @Transactional(readOnly = true)
    public ResearchTextThemeStatisticsVO textThemeStatistics(Long publishId, ResearchQueryFilter filter) {
        Dataset dataset = loadFilteredDataset(publishId, filter);
        List<ResearchTextThemeStatisticVO> rows = dataset.questions.stream()
                .filter(question -> isTextType(question.getQuestionType()))
                .map(question -> {
                    QuestionAgg agg = dataset.questionAgg.getOrDefault(question.getId(), QuestionAgg.empty());
                    return new ResearchTextThemeStatisticVO(
                            question.getId(),
                            question.getSortOrder(),
                            questionCode(question),
                            question.getQuestionType(),
                            agg.answered,
                            agg.skipped,
                            "NOT_STARTED"
                    );
                })
                .toList();
        return new ResearchTextThemeStatisticsVO(meta(dataset), rows);
    }

    public String formatParticipantCode(Long participantId) {
        if (participantId == null) {
            return "P-000000";
        }
        return "P-" + String.format(Locale.ROOT, "%06d", participantId);
    }

    Dataset loadFilteredDataset(Long publishId, ResearchQueryFilter filter) {
        return loadDataset(accessService.requireAccessibleResearchPublish(publishId), filter);
    }

    ResearchExportMaterial loadExportMaterial(Long publishId, ResearchQueryFilter filter) {
        Dataset dataset = loadFilteredDataset(publishId, filter);
        List<Long> attemptIds = dataset.filteredAttempts.stream().map(AssessmentAttemptEntity::getId).toList();
        List<AssessmentAttemptAnswerEntity> answers = attemptIds.isEmpty() ? List.of() : answerMapper.selectList(
                Wrappers.<AssessmentAttemptAnswerEntity>lambdaQuery()
                        .in(AssessmentAttemptAnswerEntity::getAttemptId, attemptIds)
                        .orderByAsc(AssessmentAttemptAnswerEntity::getAttemptId)
                        .orderByAsc(AssessmentAttemptAnswerEntity::getQuestionOrder)
                        .orderByAsc(AssessmentAttemptAnswerEntity::getId));
        String releaseCode = dataset.access.release() == null ? null : dataset.access.release().getReleaseCode();
        Map<Long, AssessmentParticipantEntity> participantsByAttempt = new LinkedHashMap<>(dataset.participantsByAttempt);
        for (AssessmentAttemptEntity attempt : dataset.filteredAttempts) {
            if (attempt.getParticipantId() == null || participantsByAttempt.containsKey(attempt.getId())) {
                continue;
            }
            AssessmentParticipantEntity participant = dataset.participantsById.get(attempt.getParticipantId());
            if (participant != null) {
                participantsByAttempt.put(attempt.getId(), participant);
            }
        }
        return new ResearchExportMaterial(
                dataset.access.paper().getTitle(),
                releaseCode,
                dataset.filteredAttempts,
                participantsByAttempt,
                dataset.metricsByAttempt,
                dataset.analysesByAttempt,
                dataset.filesByAttempt,
                answers,
                dataset.questions
        );
    }

    record ResearchExportMaterial(
            String paperTitle,
            String releaseCode,
            List<AssessmentAttemptEntity> attempts,
            Map<Long, AssessmentParticipantEntity> participantsByAttempt,
            Map<Long, AssessmentMetricSnapshotEntity> metricsByAttempt,
            Map<Long, AssessmentAiAnalysisEntity> analysesByAttempt,
            Map<Long, List<AssessmentSubmissionFileEntity>> filesByAttempt,
            List<AssessmentAttemptAnswerEntity> answers,
            List<AssessmentQuestionEntity> questions
    ) {
    }

    Dataset loadDataset(ResearchAccessService.ResearchPublishAccess access, ResearchQueryFilter filter) {
        Long publishId = access.publish().getId();
        List<AssessmentAttemptEntity> attempts = loadAttempts(List.of(publishId));
        List<AssessmentParticipantEntity> participants = participantMapper.selectList(
                Wrappers.<AssessmentParticipantEntity>lambdaQuery()
                        .eq(AssessmentParticipantEntity::getPublishId, publishId));
        Map<Long, AssessmentParticipantEntity> participantsById = participants.stream()
                .collect(Collectors.toMap(AssessmentParticipantEntity::getId, Function.identity(), (left, right) -> left));
        Map<Long, AssessmentParticipantEntity> participantsByAttempt = participants.stream()
                .filter(participant -> participant.getAttemptId() != null)
                .collect(Collectors.toMap(AssessmentParticipantEntity::getAttemptId, Function.identity(), (left, right) -> left));
        List<Long> attemptIds = attempts.stream().map(AssessmentAttemptEntity::getId).toList();
        List<AssessmentMetricSnapshotEntity> metrics = attemptIds.isEmpty() ? List.of() : metricSnapshotMapper.selectList(
                Wrappers.<AssessmentMetricSnapshotEntity>lambdaQuery()
                        .in(AssessmentMetricSnapshotEntity::getAttemptId, attemptIds)
                        .orderByDesc(AssessmentMetricSnapshotEntity::getId));
        Map<Long, AssessmentMetricSnapshotEntity> metricsByAttempt = new LinkedHashMap<>();
        for (AssessmentMetricSnapshotEntity metric : metrics) {
            metricsByAttempt.putIfAbsent(metric.getAttemptId(), metric);
        }
        List<AssessmentAiAnalysisEntity> analyses = attemptIds.isEmpty() ? List.of() : aiAnalysisMapper.selectList(
                Wrappers.<AssessmentAiAnalysisEntity>lambdaQuery()
                        .in(AssessmentAiAnalysisEntity::getAttemptId, attemptIds)
                        .orderByDesc(AssessmentAiAnalysisEntity::getId));
        Map<Long, AssessmentAiAnalysisEntity> analysesByAttempt = new LinkedHashMap<>();
        for (AssessmentAiAnalysisEntity analysis : analyses) {
            analysesByAttempt.putIfAbsent(analysis.getAttemptId(), analysis);
        }
        List<AssessmentSubmissionFileEntity> files = attemptIds.isEmpty() ? List.of() : fileMapper.selectList(
                Wrappers.<AssessmentSubmissionFileEntity>lambdaQuery()
                        .in(AssessmentSubmissionFileEntity::getAttemptId, attemptIds)
                        .ne(AssessmentSubmissionFileEntity::getBindingStatus, AssessmentFileBindingStatus.DELETED.name()));
        Map<Long, List<AssessmentSubmissionFileEntity>> filesByAttempt = files.stream()
                .collect(Collectors.groupingBy(AssessmentSubmissionFileEntity::getAttemptId));
        List<AssessmentAttemptEntity> filtered = attempts.stream()
                .filter(attempt -> matchesFilter(attempt, participantOf(attempt, participantsById, participantsByAttempt),
                        metricsByAttempt.get(attempt.getId()), analysesByAttempt.get(attempt.getId()), filter))
                .toList();
        List<AssessmentAttemptEntity> statsAttempts = selectStatsAttempts(filtered, access.release());
        Set<Long> submittedAttemptIds = statsAttempts.stream()
                .filter(this::isSubmitted)
                .map(AssessmentAttemptEntity::getId)
                .collect(Collectors.toSet());
        List<AssessmentAttemptAnswerEntity> answers = submittedAttemptIds.isEmpty() ? List.of() : answerMapper.selectList(
                Wrappers.<AssessmentAttemptAnswerEntity>lambdaQuery()
                        .in(AssessmentAttemptAnswerEntity::getAttemptId, submittedAttemptIds));
        List<AssessmentQuestionEntity> questions = loadQuestionList(access.paper().getId());
        Map<Long, QuestionAgg> questionAgg = aggregateQuestions(questions, answers, statsAttempts, files);
        return new Dataset(
                access,
                filter,
                attempts,
                filtered,
                statsAttempts,
                submittedAttemptIds,
                participants,
                participantsById,
                participantsByAttempt,
                metricsByAttempt,
                analysesByAttempt,
                filesByAttempt,
                questions,
                questionAgg
        );
    }

    private Map<Long, QuestionAgg> aggregateQuestions(
            List<AssessmentQuestionEntity> questions,
            List<AssessmentAttemptAnswerEntity> answers,
            List<AssessmentAttemptEntity> attempts,
            List<AssessmentSubmissionFileEntity> files
    ) {
        Map<Long, QuestionAgg> agg = new LinkedHashMap<>();
        for (AssessmentQuestionEntity question : questions) {
            agg.put(question.getId(), QuestionAgg.empty());
        }
        Set<Long> submittedIds = attempts.stream().filter(this::isSubmitted).map(AssessmentAttemptEntity::getId).collect(Collectors.toSet());
        Map<String, Long> boundFiles = files.stream()
                .filter(file -> AssessmentFileBindingStatus.BOUND.name().equals(file.getBindingStatus()))
                .collect(Collectors.groupingBy(file -> file.getAttemptId() + ":" + file.getQuestionId(), Collectors.counting()));
        for (AssessmentAttemptAnswerEntity answer : answers) {
            if (!submittedIds.contains(answer.getAttemptId())) {
                continue;
            }
            QuestionAgg current = agg.computeIfAbsent(answer.getQuestionId(), key -> QuestionAgg.empty());
            boolean fileAnswered = boundFiles.getOrDefault(answer.getAttemptId() + ":" + answer.getQuestionId(), 0L) > 0;
            boolean answered = Boolean.TRUE.equals(answer.getAnswered()) || fileAnswered;
            if (answered) {
                current.answered++;
                if (isAutoScored(answer.getQuestionType())) {
                    current.validAnswered++;
                    if (Boolean.TRUE.equals(answer.getCorrect())) {
                        current.correct++;
                    }
                }
            } else {
                current.skipped++;
            }
            if (answer.getEffectiveDurationMs() != null && answer.getEffectiveDurationMs() > 0) {
                current.reactionTimes.add(answer.getEffectiveDurationMs());
                if (answer.getEffectiveDurationMs() < 300) {
                    current.fastItem = true;
                }
            }
            for (String option : jsonCodec.readStringList(answer.getResponseJson())) {
                current.optionCounts.merge(option.toUpperCase(Locale.ROOT), 1L, Long::sum);
            }
        }
        long submitted = submittedIds.size();
        for (QuestionAgg current : agg.values()) {
            if (current.answered + current.skipped < submitted) {
                current.skipped = submitted - current.answered;
            }
        }
        return agg;
    }

    private boolean matchesFilter(
            AssessmentAttemptEntity attempt,
            AssessmentParticipantEntity participant,
            AssessmentMetricSnapshotEntity metric,
            AssessmentAiAnalysisEntity analysis,
            ResearchQueryFilter filter
    ) {
        if (filter.status() != null && !filter.status().equalsIgnoreCase(attempt.getStatus())) {
            return false;
        }
        if (filter.entryType() != null && (participant == null || !filter.entryType().equalsIgnoreCase(participant.getParticipantType()))) {
            return false;
        }
        if (filter.submittedFrom() != null && (attempt.getSubmittedAt() == null || attempt.getSubmittedAt().isBefore(filter.submittedFrom()))) {
            return false;
        }
        if (filter.submittedTo() != null && (attempt.getSubmittedAt() == null || attempt.getSubmittedAt().isAfter(filter.submittedTo()))) {
            return false;
        }
        if (filter.aiStatus() != null && (analysis == null || !filter.aiStatus().equalsIgnoreCase(analysis.getStatus()))) {
            return false;
        }
        if (filter.qualityFlag() != null) {
            List<String> flags = metric == null ? List.of() : jsonCodec.readStringList(metric.getQualityFlagsJson());
            if (flags.stream().noneMatch(flag -> filter.qualityFlag().equalsIgnoreCase(flag))) {
                return false;
            }
        }
        if (filter.keyword() != null) {
            Long participantId = filter.keywordParticipantId();
            if (participantId == null || !Objects.equals(attempt.getParticipantId(), participantId)) {
                return false;
            }
        }
        return true;
    }

    private ResearchAttemptSummaryVO toAttemptSummary(AssessmentAttemptEntity attempt, Dataset dataset) {
        AssessmentParticipantEntity participant = participantOf(attempt, dataset.participantsById, dataset.participantsByAttempt);
        AssessmentMetricSnapshotEntity metric = dataset.metricsByAttempt.get(attempt.getId());
        AssessmentAiAnalysisEntity analysis = dataset.analysesByAttempt.get(attempt.getId());
        List<AssessmentSubmissionFileEntity> files = dataset.filesByAttempt.getOrDefault(attempt.getId(), List.of());
        return new ResearchAttemptSummaryVO(
                attempt.getId(),
                PublicAssessmentService.attemptNoOf(attempt),
                formatParticipantCode(attempt.getParticipantId()),
                participant == null ? null : participant.getParticipantType(),
                attempt.getStatus(),
                attempt.getAnsweredCount(),
                dataset.access.publish().getQuestionCountSnapshot(),
                metric == null || metric.getPercentageScore() == null ? null : metric.getPercentageScore().doubleValue(),
                effectiveDuration(attempt),
                metric == null ? List.of() : jsonCodec.readStringList(metric.getQualityFlagsJson()),
                (int) files.stream().filter(file -> AssessmentFileBindingStatus.BOUND.name().equals(file.getBindingStatus())).count(),
                analysis == null ? null : analysis.getStatus(),
                attempt.getStartedAt(),
                attempt.getLastSavedAt(),
                attempt.getSubmittedAt()
        );
    }

    private List<ResearchAttemptSummaryVO> sortAttempts(List<ResearchAttemptSummaryVO> rows, String sort) {
        String normalized = sort == null ? "submittedAt,desc" : sort.trim();
        String[] parts = normalized.split(",");
        String field = parts[0].trim();
        boolean desc = parts.length < 2 || !"asc".equalsIgnoreCase(parts[1].trim());
        Comparator<ResearchAttemptSummaryVO> comparator = switch (field) {
            case "participantCode" -> Comparator.comparing(ResearchAttemptSummaryVO::participantCode, Comparator.nullsLast(String::compareToIgnoreCase));
            case "status" -> Comparator.comparing(ResearchAttemptSummaryVO::status, Comparator.nullsLast(String::compareToIgnoreCase));
            case "percentageScore" -> Comparator.comparing(ResearchAttemptSummaryVO::percentageScore, Comparator.nullsLast(Double::compareTo));
            case "startedAt" -> Comparator.comparing(ResearchAttemptSummaryVO::startedAt, Comparator.nullsLast(LocalDateTime::compareTo));
            default -> Comparator.comparing(ResearchAttemptSummaryVO::submittedAt, Comparator.nullsLast(LocalDateTime::compareTo));
        };
        List<ResearchAttemptSummaryVO> copy = new ArrayList<>(rows);
        copy.sort(desc ? comparator.reversed() : comparator);
        return copy;
    }

    private ResearchDataQualityOverviewVO qualityOverview(Dataset dataset) {
        long flagged = 0;
        Map<String, Long> flags = new LinkedHashMap<>();
        long considered = 0;
        for (AssessmentAttemptEntity attempt : dataset.statsAttempts) {
            if (!isSubmitted(attempt)) {
                continue;
            }
            considered++;
            List<String> qualityFlags = dataset.metricsByAttempt.get(attempt.getId()) == null
                    ? List.of()
                    : jsonCodec.readStringList(dataset.metricsByAttempt.get(attempt.getId()).getQualityFlagsJson());
            if (!qualityFlags.isEmpty()) {
                flagged++;
            }
            for (String flag : qualityFlags) {
                flags.merge(flag, 1L, Long::sum);
            }
        }
        return new ResearchDataQualityOverviewVO(
                Math.max(0, considered - flagged),
                flagged,
                flags.entrySet().stream().map(entry -> new ResearchFlagCountVO(entry.getKey(), entry.getValue())).toList()
        );
    }

    private ResearchAiStatusOverviewVO aiOverview(Dataset dataset) {
        long pending = 0;
        long processing = 0;
        long completed = 0;
        long fallback = 0;
        long failed = 0;
        for (AssessmentAttemptEntity attempt : dataset.statsAttempts) {
            AssessmentAiAnalysisEntity analysis = dataset.analysesByAttempt.get(attempt.getId());
            if (analysis == null) {
                continue;
            }
            switch (analysis.getStatus() == null ? "" : analysis.getStatus().toUpperCase(Locale.ROOT)) {
                case "PENDING" -> pending++;
                case "PROCESSING" -> processing++;
                case "COMPLETED" -> completed++;
                case "FALLBACK" -> fallback++;
                case "FAILED" -> failed++;
                default -> {
                }
            }
        }
        return new ResearchAiStatusOverviewVO(pending, processing, completed, fallback, failed);
    }

    private ResearchStatisticsMetaVO meta(Dataset dataset) {
        return new ResearchStatisticsMetaVO(
                dataset.filter.echo(),
                dataset.submittedAttemptIds.size(),
                LocalDateTime.now(),
                properties.getStatisticsVersion()
        );
    }

    private Map<Long, String> latestReportStatus(List<Long> publishIds) {
        if (publishIds.isEmpty()) {
            return Map.of();
        }
        List<ResearchAiReportEntity> reports = reportMapper.selectList(Wrappers.<ResearchAiReportEntity>lambdaQuery()
                .in(ResearchAiReportEntity::getPublishId, publishIds)
                .orderByDesc(ResearchAiReportEntity::getRequestedAt)
                .orderByDesc(ResearchAiReportEntity::getId));
        Map<Long, String> latest = new HashMap<>();
        for (ResearchAiReportEntity report : reports) {
            latest.putIfAbsent(report.getPublishId(), report.getStatus());
        }
        return latest;
    }

    private AssessmentParticipantEntity participantOf(
            AssessmentAttemptEntity attempt,
            Map<Long, AssessmentParticipantEntity> participantsById,
            Map<Long, AssessmentParticipantEntity> participantsByAttempt
    ) {
        if (attempt.getParticipantId() != null) {
            AssessmentParticipantEntity byId = participantsById.get(attempt.getParticipantId());
            if (byId != null) {
                return byId;
            }
        }
        return participantsByAttempt.get(attempt.getId());
    }

    private List<AssessmentAttemptEntity> selectStatsAttempts(
            List<AssessmentAttemptEntity> filtered,
            AssessmentPublicReleaseEntity release
    ) {
        if (AssessmentPublicReleaseManagementService.resolveMaxAttempts(release == null ? null : release.getMaxAttempts()) <= 1) {
            return filtered;
        }
        Map<Long, AssessmentAttemptEntity> latestSubmitted = new LinkedHashMap<>();
        List<AssessmentAttemptEntity> remainder = new ArrayList<>();
        for (AssessmentAttemptEntity attempt : filtered) {
            Long participantId = attempt.getParticipantId();
            if (participantId == null || !isSubmitted(attempt)) {
                remainder.add(attempt);
                continue;
            }
            AssessmentAttemptEntity current = latestSubmitted.get(participantId);
            if (current == null || isLaterAttempt(attempt, current)) {
                latestSubmitted.put(participantId, attempt);
            }
        }
        List<AssessmentAttemptEntity> selected = new ArrayList<>(latestSubmitted.values());
        selected.addAll(remainder);
        return selected;
    }

    private boolean isLaterAttempt(AssessmentAttemptEntity left, AssessmentAttemptEntity right) {
        int leftNo = PublicAssessmentService.attemptNoOf(left);
        int rightNo = PublicAssessmentService.attemptNoOf(right);
        if (leftNo != rightNo) {
            return leftNo > rightNo;
        }
        if (left.getSubmittedAt() != null && right.getSubmittedAt() != null
                && !left.getSubmittedAt().equals(right.getSubmittedAt())) {
            return left.getSubmittedAt().isAfter(right.getSubmittedAt());
        }
        return left.getId() != null && right.getId() != null && left.getId() > right.getId();
    }

    private long uniqueParticipantCount(List<AssessmentAttemptEntity> attempts) {
        return attempts.stream()
                .map(AssessmentAttemptEntity::getParticipantId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private List<AssessmentAttemptEntity> loadAttempts(Collection<Long> publishIds) {
        if (publishIds.isEmpty()) {
            return List.of();
        }
        return attemptMapper.selectList(Wrappers.<AssessmentAttemptEntity>lambdaQuery()
                .in(AssessmentAttemptEntity::getPublishId, publishIds)
                .orderByDesc(AssessmentAttemptEntity::getSubmittedAt)
                .orderByDesc(AssessmentAttemptEntity::getId));
    }

    private Map<Long, AssessmentQuestionEntity> loadQuestions(Long paperId) {
        return loadQuestionList(paperId).stream()
                .collect(Collectors.toMap(AssessmentQuestionEntity::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
    }

    private List<AssessmentQuestionEntity> loadQuestionList(Long paperId) {
        return questionMapper.selectList(Wrappers.<AssessmentQuestionEntity>lambdaQuery()
                .eq(AssessmentQuestionEntity::getPaperId, paperId)
                .orderByAsc(AssessmentQuestionEntity::getSortOrder)
                .orderByAsc(AssessmentQuestionEntity::getId));
    }

    private boolean isSubmitted(AssessmentAttemptEntity attempt) {
        return "SUBMITTED".equalsIgnoreCase(attempt.getStatus());
    }

    private boolean isExpired(AssessmentAttemptEntity attempt) {
        return isSubmitted(attempt) && AssessmentSubmitReason.TIMEOUT.name().equalsIgnoreCase(attempt.getSubmitReason());
    }

    private Long effectiveDuration(AssessmentAttemptEntity attempt) {
        if (attempt.getStartedAt() == null || attempt.getSubmittedAt() == null) {
            return null;
        }
        return Math.max(0, java.time.Duration.between(attempt.getStartedAt(), attempt.getSubmittedAt()).toMillis());
    }

    private ResearchAttachmentVO toAttachment(AssessmentSubmissionFileEntity file) {
        boolean downloadable = AssessmentFileScanStatus.CLEAN.name().equalsIgnoreCase(file.getScanStatus())
                && !AssessmentFileBindingStatus.DELETED.name().equalsIgnoreCase(file.getBindingStatus());
        return new ResearchAttachmentVO(
                file.getId(),
                null,
                file.getOriginalFileName(),
                file.getMimeType(),
                file.getFileExtension(),
                file.getSizeBytes(),
                file.getScanStatus(),
                file.getBindingStatus(),
                file.getUploadedAt(),
                downloadable
        );
    }

    private AssessmentAiAnalysisVO decodeAnalysis(AssessmentAiAnalysisEntity analysis) {
        String payload = analysis.getAnalysisJson() == null || analysis.getAnalysisJson().isBlank()
                ? analysis.getRuleFallbackJson()
                : analysis.getAnalysisJson();
        if (payload == null || payload.isBlank()) {
            return null;
        }
        return jsonCodec.read(payload, AssessmentAiAnalysisVO.class);
    }

    private boolean isAutoScored(String questionType) {
        if (questionType == null) {
            return false;
        }
        try {
            AssessmentQuestionType type = AssessmentQuestionType.fromCode(questionType);
            return type == AssessmentQuestionType.SINGLE_CHOICE
                    || type == AssessmentQuestionType.MULTIPLE_CHOICE
                    || type == AssessmentQuestionType.TRUE_FALSE
                    || type == AssessmentQuestionType.TRUE_FALSE_WITH_JUSTIFICATION
                    || type == AssessmentQuestionType.FILL_BLANK
                    || type == AssessmentQuestionType.SPELLING;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isChoiceType(String questionType) {
        try {
            AssessmentQuestionType type = AssessmentQuestionType.fromCode(questionType);
            return type == AssessmentQuestionType.SINGLE_CHOICE
                    || type == AssessmentQuestionType.MULTIPLE_CHOICE
                    || type == AssessmentQuestionType.TRUE_FALSE
                    || type == AssessmentQuestionType.TRUE_FALSE_WITH_JUSTIFICATION
                    || type == AssessmentQuestionType.INFORMED_CONSENT;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isTextType(String questionType) {
        try {
            AssessmentQuestionType type = AssessmentQuestionType.fromCode(questionType);
            return type == AssessmentQuestionType.SHORT_TEXT
                    || type == AssessmentQuestionType.NUMBER
                    || type == AssessmentQuestionType.FILE_UPLOAD;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean resolveFormalSection(AssessmentQuestionEntity question, String questionType) {
        String sectionCode = question == null ? null : question.getSectionCode();
        if (sectionCode != null && sectionCode.toUpperCase(Locale.ROOT).startsWith("BASIC")) {
            return false;
        }
        if ("INSTRUCTION".equalsIgnoreCase(questionType)
                && (sectionCode == null || sectionCode.toUpperCase(Locale.ROOT).contains("BASIC"))) {
            return false;
        }
        return true;
    }

    private String questionCode(AssessmentQuestionEntity question) {
        if (question.getTargetWord() != null && !question.getTargetWord().isBlank()) {
            return question.getTargetWord();
        }
        return "Q" + question.getSortOrder();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private ResearchDistributionStatsVO distributionFromLongs(List<Long> values) {
        if (values.isEmpty()) {
            return new ResearchDistributionStatsVO(null, null, null, null, null, 0);
        }
        double average = values.stream().mapToLong(Long::longValue).average().orElse(0);
        return new ResearchDistributionStatsVO(
                average,
                percentile(values, 50),
                percentile(values, 25),
                percentile(values, 75),
                percentile(values, 90),
                values.size()
        );
    }

    private ResearchDistributionStatsVO distributionFromDoubles(List<Double> values) {
        if (values.isEmpty()) {
            return new ResearchDistributionStatsVO(null, null, null, null, null, 0);
        }
        List<Long> scaled = values.stream().map(value -> Math.round(value * 100d)).toList();
        double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return new ResearchDistributionStatsVO(
                average,
                percentile(scaled, 50) == null ? null : Math.round(percentile(scaled, 50) / 100d),
                percentile(scaled, 25) == null ? null : Math.round(percentile(scaled, 25) / 100d),
                percentile(scaled, 75) == null ? null : Math.round(percentile(scaled, 75) / 100d),
                percentile(scaled, 90) == null ? null : Math.round(percentile(scaled, 90) / 100d),
                values.size()
        );
    }

    private Long percentile(List<Long> sorted, int percentile) {
        if (sorted == null || sorted.isEmpty()) {
            return null;
        }
        int index = (int) Math.ceil(percentile / 100d * sorted.size()) - 1;
        index = Math.max(0, Math.min(sorted.size() - 1, index));
        return sorted.get(index);
    }

    static final class QuestionAgg {
        long answered;
        long skipped;
        long validAnswered;
        long correct;
        boolean fastItem;
        final List<Long> reactionTimes = new ArrayList<>();
        final Map<String, Long> optionCounts = new LinkedHashMap<>();

        static QuestionAgg empty() {
            return new QuestionAgg();
        }
    }

    static final class Dataset {
        final ResearchAccessService.ResearchPublishAccess access;
        final ResearchQueryFilter filter;
        final List<AssessmentAttemptEntity> allAttempts;
        final List<AssessmentAttemptEntity> filteredAttempts;
        final List<AssessmentAttemptEntity> statsAttempts;
        final Set<Long> submittedAttemptIds;
        final List<AssessmentParticipantEntity> allParticipants;
        final Map<Long, AssessmentParticipantEntity> participantsById;
        final Map<Long, AssessmentParticipantEntity> participantsByAttempt;
        final Map<Long, AssessmentMetricSnapshotEntity> metricsByAttempt;
        final Map<Long, AssessmentAiAnalysisEntity> analysesByAttempt;
        final Map<Long, List<AssessmentSubmissionFileEntity>> filesByAttempt;
        final List<AssessmentQuestionEntity> questions;
        final Map<Long, QuestionAgg> questionAgg;

        Dataset(
                ResearchAccessService.ResearchPublishAccess access,
                ResearchQueryFilter filter,
                List<AssessmentAttemptEntity> allAttempts,
                List<AssessmentAttemptEntity> filteredAttempts,
                List<AssessmentAttemptEntity> statsAttempts,
                Set<Long> submittedAttemptIds,
                List<AssessmentParticipantEntity> allParticipants,
                Map<Long, AssessmentParticipantEntity> participantsById,
                Map<Long, AssessmentParticipantEntity> participantsByAttempt,
                Map<Long, AssessmentMetricSnapshotEntity> metricsByAttempt,
                Map<Long, AssessmentAiAnalysisEntity> analysesByAttempt,
                Map<Long, List<AssessmentSubmissionFileEntity>> filesByAttempt,
                List<AssessmentQuestionEntity> questions,
                Map<Long, QuestionAgg> questionAgg
        ) {
            this.access = access;
            this.filter = filter;
            this.allAttempts = allAttempts;
            this.filteredAttempts = filteredAttempts;
            this.statsAttempts = statsAttempts;
            this.submittedAttemptIds = submittedAttemptIds;
            this.allParticipants = allParticipants;
            this.participantsById = participantsById;
            this.participantsByAttempt = participantsByAttempt;
            this.metricsByAttempt = metricsByAttempt;
            this.analysesByAttempt = analysesByAttempt;
            this.filesByAttempt = filesByAttempt;
            this.questions = questions;
            this.questionAgg = questionAgg;
        }
    }
}
