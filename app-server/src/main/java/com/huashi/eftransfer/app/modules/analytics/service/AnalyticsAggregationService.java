package com.huashi.eftransfer.app.modules.analytics.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.analytics.entity.AnalyticsDailyAggregateEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.ClassAnalyticsDailyAggregateEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.LearningProfileSnapshotEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.AnalyticsDailyAggregateMapper;
import com.huashi.eftransfer.app.modules.analytics.mapper.ClassAnalyticsDailyAggregateMapper;
import com.huashi.eftransfer.app.modules.analytics.mapper.LearningProfileSnapshotMapper;
import com.huashi.eftransfer.app.modules.analytics.mapper.TeachingClassMapper;
import com.huashi.eftransfer.app.modules.analytics.support.AnalyticsConstants;
import com.huashi.eftransfer.app.modules.analytics.support.AnalyticsJsonCodec;
import com.huashi.eftransfer.app.modules.analytics.support.ClassAnalyticsSnapshotPayload;
import com.huashi.eftransfer.app.modules.analytics.support.StudentAnalyticsSnapshotPayload;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisItemResultEntity;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisSummaryEntity;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisTemplateItemEntity;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisItemResultMapper;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisSummaryMapper;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisTemplateItemMapper;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisHighRiskLexicalPair;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisJsonCodec;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairMapper;
import com.huashi.eftransfer.app.modules.training.entity.ReviewScheduleEntity;
import com.huashi.eftransfer.app.modules.training.entity.TrainingItemResultEntity;
import com.huashi.eftransfer.app.modules.training.entity.TrainingPlanItemEntity;
import com.huashi.eftransfer.app.modules.training.entity.TrainingSessionEntity;
import com.huashi.eftransfer.app.modules.training.mapper.ReviewScheduleMapper;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingItemResultMapper;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingPlanItemMapper;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingSessionMapper;
import com.huashi.eftransfer.app.modules.training.support.TrainingJsonCodec;
import com.huashi.eftransfer.app.modules.training.support.TrainingLearningProfileSnapshot;
import com.huashi.eftransfer.app.modules.training.support.TrainingRiskWordSnapshot;
import com.huashi.eftransfer.app.modules.training.support.TrainingSessionSummarySnapshot;
import com.huashi.eftransfer.app.modules.training.support.TrainingStimulusPayload;
import com.huashi.eftransfer.app.modules.user.entity.StudentProfileEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.shared.enums.ContextSupportLevel;
import com.huashi.eftransfer.shared.enums.LexicalPairType;
import com.huashi.eftransfer.shared.enums.RiskLevel;
import com.huashi.eftransfer.shared.enums.ReviewScheduleStatus;
import com.huashi.eftransfer.shared.enums.TrainingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnalyticsAggregationService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsAggregationService.class);
    private static final int SNAPSHOT_RANGE_DAYS = 30;

    private final AnalyticsDailyAggregateMapper analyticsDailyAggregateMapper;
    private final ClassAnalyticsDailyAggregateMapper classAnalyticsDailyAggregateMapper;
    private final LearningProfileSnapshotMapper learningProfileSnapshotMapper;
    private final TeachingClassMapper teachingClassMapper;
    private final TeachingClassService teachingClassService;
    private final DiagnosisSummaryMapper diagnosisSummaryMapper;
    private final DiagnosisItemResultMapper diagnosisItemResultMapper;
    private final DiagnosisTemplateItemMapper diagnosisTemplateItemMapper;
    private final TrainingSessionMapper trainingSessionMapper;
    private final TrainingItemResultMapper trainingItemResultMapper;
    private final TrainingPlanItemMapper trainingPlanItemMapper;
    private final ReviewScheduleMapper reviewScheduleMapper;
    private final LexicalPairMapper lexicalPairMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final UserMapper userMapper;
    private final DiagnosisJsonCodec diagnosisJsonCodec;
    private final TrainingJsonCodec trainingJsonCodec;
    private final AnalyticsJsonCodec analyticsJsonCodec;

    public AnalyticsAggregationService(
            AnalyticsDailyAggregateMapper analyticsDailyAggregateMapper,
            ClassAnalyticsDailyAggregateMapper classAnalyticsDailyAggregateMapper,
            LearningProfileSnapshotMapper learningProfileSnapshotMapper,
            TeachingClassMapper teachingClassMapper,
            TeachingClassService teachingClassService,
            DiagnosisSummaryMapper diagnosisSummaryMapper,
            DiagnosisItemResultMapper diagnosisItemResultMapper,
            DiagnosisTemplateItemMapper diagnosisTemplateItemMapper,
            TrainingSessionMapper trainingSessionMapper,
            TrainingItemResultMapper trainingItemResultMapper,
            TrainingPlanItemMapper trainingPlanItemMapper,
            ReviewScheduleMapper reviewScheduleMapper,
            LexicalPairMapper lexicalPairMapper,
            StudentProfileMapper studentProfileMapper,
            UserMapper userMapper,
            DiagnosisJsonCodec diagnosisJsonCodec,
            TrainingJsonCodec trainingJsonCodec,
            AnalyticsJsonCodec analyticsJsonCodec
    ) {
        this.analyticsDailyAggregateMapper = analyticsDailyAggregateMapper;
        this.classAnalyticsDailyAggregateMapper = classAnalyticsDailyAggregateMapper;
        this.learningProfileSnapshotMapper = learningProfileSnapshotMapper;
        this.teachingClassMapper = teachingClassMapper;
        this.teachingClassService = teachingClassService;
        this.diagnosisSummaryMapper = diagnosisSummaryMapper;
        this.diagnosisItemResultMapper = diagnosisItemResultMapper;
        this.diagnosisTemplateItemMapper = diagnosisTemplateItemMapper;
        this.trainingSessionMapper = trainingSessionMapper;
        this.trainingItemResultMapper = trainingItemResultMapper;
        this.trainingPlanItemMapper = trainingPlanItemMapper;
        this.reviewScheduleMapper = reviewScheduleMapper;
        this.lexicalPairMapper = lexicalPairMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.userMapper = userMapper;
        this.diagnosisJsonCodec = diagnosisJsonCodec;
        this.trainingJsonCodec = trainingJsonCodec;
        this.analyticsJsonCodec = analyticsJsonCodec;
    }

    @Transactional
    public void aggregateFromDiagnosisSummary(Long summaryId) {
        DiagnosisSummaryEntity summary = diagnosisSummaryMapper.selectById(summaryId);
        if (summary == null || summary.getGeneratedAt() == null) {
            return;
        }
        rebuildForOwnerDay(summary.getOwnerUserId(), summary.getGeneratedAt(), AnalyticsConstants.SOURCE_DIAGNOSIS);
        log.info("event=analytics_aggregate_from_diagnosis summaryId={} ownerUserId={}",
                summaryId, summary.getOwnerUserId());
    }

    @Transactional
    public void aggregateFromTrainingSession(Long sessionId) {
        TrainingSessionEntity session = trainingSessionMapper.selectById(sessionId);
        if (session == null || session.getCompletedAt() == null) {
            return;
        }
        rebuildForOwnerDay(session.getOwnerUserId(), session.getCompletedAt(), AnalyticsConstants.SOURCE_TRAINING);
        log.info("event=analytics_aggregate_from_training sessionId={} ownerUserId={}",
                sessionId, session.getOwnerUserId());
    }

    @Transactional
    public void rebuildRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || startDate.isAfter(endDate)) {
            return;
        }

        Set<RebuildTask> tasks = new LinkedHashSet<>();
        diagnosisSummaryMapper.selectList(Wrappers.<DiagnosisSummaryEntity>lambdaQuery()
                        .ge(DiagnosisSummaryEntity::getGeneratedAt, startDate.atStartOfDay())
                        .lt(DiagnosisSummaryEntity::getGeneratedAt, endDate.plusDays(1).atStartOfDay())
                        .orderByAsc(DiagnosisSummaryEntity::getGeneratedAt)
                        .orderByAsc(DiagnosisSummaryEntity::getId))
                .forEach(summary -> tasks.add(new RebuildTask(
                        summary.getOwnerUserId(),
                        summary.getGeneratedAt(),
                        AnalyticsConstants.SOURCE_DIAGNOSIS
                )));
        trainingSessionMapper.selectList(Wrappers.<TrainingSessionEntity>lambdaQuery()
                        .isNotNull(TrainingSessionEntity::getCompletedAt)
                        .ge(TrainingSessionEntity::getCompletedAt, startDate.atStartOfDay())
                        .lt(TrainingSessionEntity::getCompletedAt, endDate.plusDays(1).atStartOfDay())
                        .orderByAsc(TrainingSessionEntity::getCompletedAt)
                        .orderByAsc(TrainingSessionEntity::getId))
                .forEach(session -> tasks.add(new RebuildTask(
                        session.getOwnerUserId(),
                        session.getCompletedAt(),
                        AnalyticsConstants.SOURCE_TRAINING
                )));

        for (RebuildTask task : tasks) {
            rebuildForOwnerDay(task.ownerUserId(), task.eventAt(), task.sourceType());
        }
    }

    private void rebuildForOwnerDay(Long ownerUserId, LocalDateTime eventAt, String sourceType) {
        LocalDate statDate = eventAt.toLocalDate();
        rebuildStudentDailyAggregate(ownerUserId, statDate, sourceType);
        List<Long> classIds = teachingClassService.listActiveClassIdsByStudent(ownerUserId, eventAt);
        for (Long classId : classIds) {
            rebuildClassDailyAggregate(classId, statDate, sourceType, eventAt);
        }
        rebuildStudentSnapshot(ownerUserId);
        syncLegacyStudentProfile(ownerUserId);
        for (Long classId : classIds) {
            rebuildClassSnapshot(classId);
        }
    }

    private void rebuildStudentDailyAggregate(Long ownerUserId, LocalDate statDate, String sourceType) {
        analyticsDailyAggregateMapper.hardDeleteByOwnerDateAndSource(ownerUserId, statDate, sourceType);

        Map<AggregateKey, AggregateAccumulator> accumulators = AnalyticsConstants.SOURCE_DIAGNOSIS.equalsIgnoreCase(sourceType)
                ? buildDiagnosisAccumulators(ownerUserId, statDate)
                : buildTrainingAccumulators(ownerUserId, statDate);
        for (AggregateAccumulator accumulator : accumulators.values()) {
            analyticsDailyAggregateMapper.insert(accumulator.toStudentEntity(ownerUserId, statDate));
        }
    }

    private void rebuildClassDailyAggregate(Long classId, LocalDate statDate, String sourceType, LocalDateTime asOf) {
        classAnalyticsDailyAggregateMapper.hardDeleteByClassDateAndSource(classId, statDate, sourceType);

        List<Long> studentIds = teachingClassService.listActiveStudentIds(classId, asOf);
        if (studentIds.isEmpty()) {
            return;
        }

        List<AnalyticsDailyAggregateEntity> studentRows = analyticsDailyAggregateMapper.selectList(Wrappers.<AnalyticsDailyAggregateEntity>lambdaQuery()
                .in(AnalyticsDailyAggregateEntity::getOwnerUserId, studentIds)
                .eq(AnalyticsDailyAggregateEntity::getStatDate, statDate)
                .eq(AnalyticsDailyAggregateEntity::getSourceType, sourceType)
                .orderByAsc(AnalyticsDailyAggregateEntity::getId));
        Map<AggregateKey, AggregateAccumulator> accumulators = new LinkedHashMap<>();
        for (AnalyticsDailyAggregateEntity row : studentRows) {
            AggregateKey key = new AggregateKey(
                    row.getSourceType(),
                    row.getAggregationLevel(),
                    row.getLexicalPairId(),
                    row.getLexicalPairType(),
                    row.getTrainingMode(),
                    row.getContextSupportLevel()
            );
            accumulators.computeIfAbsent(key, AggregateAccumulator::new)
                    .mergeStudentRow(row);
        }
        for (AggregateAccumulator accumulator : accumulators.values()) {
            classAnalyticsDailyAggregateMapper.insert(accumulator.toClassEntity(classId, statDate));
        }
    }

    private Map<AggregateKey, AggregateAccumulator> buildDiagnosisAccumulators(Long ownerUserId, LocalDate statDate) {
        List<DiagnosisSummaryEntity> summaries = diagnosisSummaryMapper.selectList(Wrappers.<DiagnosisSummaryEntity>lambdaQuery()
                .eq(DiagnosisSummaryEntity::getOwnerUserId, ownerUserId)
                .ge(DiagnosisSummaryEntity::getGeneratedAt, statDate.atStartOfDay())
                .lt(DiagnosisSummaryEntity::getGeneratedAt, statDate.plusDays(1).atStartOfDay())
                .orderByAsc(DiagnosisSummaryEntity::getGeneratedAt)
                .orderByAsc(DiagnosisSummaryEntity::getId));
        if (summaries.isEmpty()) {
            return Map.of();
        }

        List<Long> sessionIds = summaries.stream()
                .map(DiagnosisSummaryEntity::getSessionId)
                .distinct()
                .toList();
        List<DiagnosisItemResultEntity> itemResults = diagnosisItemResultMapper.selectList(Wrappers.<DiagnosisItemResultEntity>lambdaQuery()
                .in(DiagnosisItemResultEntity::getSessionId, sessionIds)
                .orderByAsc(DiagnosisItemResultEntity::getSessionId)
                .orderByAsc(DiagnosisItemResultEntity::getPresentationOrder)
                .orderByAsc(DiagnosisItemResultEntity::getId));
        Map<Long, List<DiagnosisItemResultEntity>> sessionResults = itemResults.stream()
                .collect(Collectors.groupingBy(DiagnosisItemResultEntity::getSessionId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, DiagnosisTemplateItemEntity> templateItemMap = loadDiagnosisTemplateItemMap(itemResults.stream()
                .map(DiagnosisItemResultEntity::getTemplateItemId)
                .toList());
        Map<Long, LexicalPairEntity> pairMap = loadLexicalPairMap(itemResults.stream()
                .map(DiagnosisItemResultEntity::getLexicalPairId)
                .toList());
        int pendingReviewCount = pendingReviewCount(ownerUserId);

        Map<AggregateKey, AggregateAccumulator> accumulators = new LinkedHashMap<>();
        for (DiagnosisSummaryEntity summary : summaries) {
            List<DiagnosisItemResultEntity> results = sessionResults.getOrDefault(summary.getSessionId(), List.of());
            int highRiskCount = summary.getHighRiskLexicalPairsJson() == null || summary.getHighRiskLexicalPairsJson().isBlank()
                    ? 0
                    : diagnosisJsonCodec.readHighRiskLexicalPairs(summary.getHighRiskLexicalPairsJson()).size();
            AggregateKey summaryKey = new AggregateKey(
                    AnalyticsConstants.SOURCE_DIAGNOSIS,
                    AnalyticsConstants.AGGREGATION_LEVEL_SUMMARY,
                    AnalyticsConstants.SUMMARY_LEXICAL_PAIR_ID,
                    AnalyticsConstants.DIMENSION_ALL,
                    AnalyticsConstants.DIMENSION_ALL,
                    AnalyticsConstants.DIMENSION_ALL
            );
            accumulators.computeIfAbsent(summaryKey, AggregateAccumulator::new)
                    .addDiagnosisSummary(summary, results, pendingReviewCount, highRiskCount);

            for (DiagnosisItemResultEntity result : results) {
                LexicalPairEntity pair = pairMap.get(result.getLexicalPairId());
                if (pair == null) {
                    continue;
                }
                DiagnosisTemplateItemEntity templateItem = templateItemMap.get(result.getTemplateItemId());
                AggregateKey pairKey = new AggregateKey(
                        AnalyticsConstants.SOURCE_DIAGNOSIS,
                        AnalyticsConstants.AGGREGATION_LEVEL_PAIR,
                        pair.getId(),
                        normalizePairType(pair.getLexicalPairType()),
                        AnalyticsConstants.DIMENSION_ALL,
                        normalizeContextLevel(templateItem == null ? null : templateItem.getContextSupportLevel())
                );
                accumulators.computeIfAbsent(pairKey, AggregateAccumulator::new)
                        .addDiagnosisItem(result);
            }
        }
        return accumulators;
    }

    private Map<AggregateKey, AggregateAccumulator> buildTrainingAccumulators(Long ownerUserId, LocalDate statDate) {
        List<TrainingSessionEntity> sessions = trainingSessionMapper.selectList(Wrappers.<TrainingSessionEntity>lambdaQuery()
                .eq(TrainingSessionEntity::getOwnerUserId, ownerUserId)
                .isNotNull(TrainingSessionEntity::getCompletedAt)
                .ge(TrainingSessionEntity::getCompletedAt, statDate.atStartOfDay())
                .lt(TrainingSessionEntity::getCompletedAt, statDate.plusDays(1).atStartOfDay())
                .orderByAsc(TrainingSessionEntity::getCompletedAt)
                .orderByAsc(TrainingSessionEntity::getId));
        if (sessions.isEmpty()) {
            return Map.of();
        }

        List<Long> sessionIds = sessions.stream().map(TrainingSessionEntity::getId).toList();
        List<TrainingItemResultEntity> itemResults = trainingItemResultMapper.selectList(Wrappers.<TrainingItemResultEntity>lambdaQuery()
                .in(TrainingItemResultEntity::getSessionId, sessionIds)
                .orderByAsc(TrainingItemResultEntity::getSessionId)
                .orderByAsc(TrainingItemResultEntity::getPresentationOrder)
                .orderByAsc(TrainingItemResultEntity::getId));
        Map<Long, List<TrainingItemResultEntity>> sessionResults = itemResults.stream()
                .collect(Collectors.groupingBy(TrainingItemResultEntity::getSessionId, LinkedHashMap::new, Collectors.toList()));
        Map<Long, TrainingPlanItemEntity> planItemMap = loadTrainingPlanItemMap(itemResults.stream()
                .map(TrainingItemResultEntity::getPlanItemId)
                .toList());
        Map<Long, LexicalPairEntity> pairMap = loadLexicalPairMap(itemResults.stream()
                .map(TrainingItemResultEntity::getLexicalPairId)
                .toList());
        int pendingReviewCount = pendingReviewCount(ownerUserId);

        Map<AggregateKey, AggregateAccumulator> accumulators = new LinkedHashMap<>();
        for (TrainingSessionEntity session : sessions) {
            List<TrainingItemResultEntity> results = sessionResults.getOrDefault(session.getId(), List.of());
            TrainingSessionSummarySnapshot summarySnapshot = session.getSummarySnapshotJson() == null || session.getSummarySnapshotJson().isBlank()
                    ? null
                    : trainingJsonCodec.readSummarySnapshot(session.getSummarySnapshotJson());
            AggregateKey summaryKey = new AggregateKey(
                    AnalyticsConstants.SOURCE_TRAINING,
                    AnalyticsConstants.AGGREGATION_LEVEL_SUMMARY,
                    AnalyticsConstants.SUMMARY_LEXICAL_PAIR_ID,
                    AnalyticsConstants.DIMENSION_ALL,
                    normalizeTrainingMode(session.getMode()),
                    AnalyticsConstants.DIMENSION_ALL
            );
            accumulators.computeIfAbsent(summaryKey, AggregateAccumulator::new)
                    .addTrainingSummary(session, summarySnapshot, results, pendingReviewCount);

            for (TrainingItemResultEntity result : results) {
                LexicalPairEntity pair = pairMap.get(result.getLexicalPairId());
                if (pair == null) {
                    continue;
                }
                TrainingPlanItemEntity planItem = planItemMap.get(result.getPlanItemId());
                String contextLevel = AnalyticsConstants.DIMENSION_ALL;
                if (planItem != null && planItem.getTargetContextSupport() != null && !planItem.getTargetContextSupport().isBlank()) {
                    contextLevel = normalizeContextLevel(planItem.getTargetContextSupport());
                } else if (result.getStimulusJson() != null && !result.getStimulusJson().isBlank()) {
                    TrainingStimulusPayload stimulus = trainingJsonCodec.readStimulus(result.getStimulusJson());
                    if (stimulus != null) {
                        contextLevel = normalizeContextLevel(stimulus.contextSupportLevel());
                    }
                }
                AggregateKey pairKey = new AggregateKey(
                        AnalyticsConstants.SOURCE_TRAINING,
                        AnalyticsConstants.AGGREGATION_LEVEL_PAIR,
                        pair.getId(),
                        normalizePairType(pair.getLexicalPairType()),
                        normalizeTrainingMode(result.getMode()),
                        contextLevel
                );
                accumulators.computeIfAbsent(pairKey, AggregateAccumulator::new)
                        .addTrainingItem(result);
            }
        }
        return accumulators;
    }

    private void rebuildStudentSnapshot(Long ownerUserId) {
        LocalDate startDate = LocalDate.now().minusDays(SNAPSHOT_RANGE_DAYS - 1L);
        List<AnalyticsDailyAggregateEntity> rows = analyticsDailyAggregateMapper.selectList(Wrappers.<AnalyticsDailyAggregateEntity>lambdaQuery()
                .eq(AnalyticsDailyAggregateEntity::getOwnerUserId, ownerUserId)
                .ge(AnalyticsDailyAggregateEntity::getStatDate, startDate)
                .orderByAsc(AnalyticsDailyAggregateEntity::getStatDate)
                .orderByAsc(AnalyticsDailyAggregateEntity::getId));
        List<AnalyticsDailyAggregateEntity> summaryRows = rows.stream()
                .filter(row -> AnalyticsConstants.AGGREGATION_LEVEL_SUMMARY.equalsIgnoreCase(row.getAggregationLevel()))
                .toList();
        List<AnalyticsDailyAggregateEntity> pairRows = rows.stream()
                .filter(row -> AnalyticsConstants.AGGREGATION_LEVEL_PAIR.equalsIgnoreCase(row.getAggregationLevel()))
                .toList();

        long totalAttempts = summaryRows.stream().mapToLong(row -> safeInt(row.getAttemptCount())).sum();
        long totalCorrect = summaryRows.stream().mapToLong(row -> safeInt(row.getCorrectCount())).sum();
        long totalReactionTime = summaryRows.stream().mapToLong(row -> safeLong(row.getTotalReactionTimeMs())).sum();
        long completionCount = summaryRows.stream().mapToLong(row -> safeInt(row.getCompletionCount())).sum();
        double recentAccuracy = totalAttempts == 0 ? 0d : totalCorrect / (double) totalAttempts;
        double recentNegativeRisk = completionCount == 0
                ? averagePairRisk(pairRows)
                : summaryRows.stream().mapToDouble(row -> safeDecimal(row.getNegativeTransferRiskSum())).sum() / completionCount;
        long recentAvgReactionTime = totalAttempts == 0 ? 0L : Math.round(totalReactionTime / (double) totalAttempts);
        int pendingReviewCount = pendingReviewCount(ownerUserId);
        List<PairRiskAggregate> pairRiskAggregates = buildStudentPairRiskAggregates(pairRows);
        int highRiskPairCount = (int) pairRiskAggregates.stream().filter(pair -> pair.riskScore() >= 0.6d).count();
        LocalDateTime lastActiveAt = rows.stream()
                .map(AnalyticsDailyAggregateEntity::getLastEventAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        StudentProfileEntity studentProfile = studentProfileMapper.selectOne(Wrappers.<StudentProfileEntity>lambdaQuery()
                .eq(StudentProfileEntity::getUserId, ownerUserId));
        UserEntity user = userMapper.selectById(ownerUserId);
        DiagnosisSummaryEntity latestDiagnosis = diagnosisSummaryMapper.selectOne(Wrappers.<DiagnosisSummaryEntity>lambdaQuery()
                .eq(DiagnosisSummaryEntity::getOwnerUserId, ownerUserId)
                .orderByDesc(DiagnosisSummaryEntity::getGeneratedAt)
                .orderByDesc(DiagnosisSummaryEntity::getId)
                .last("LIMIT 1"));
        TrainingSessionEntity latestTraining = trainingSessionMapper.selectOne(Wrappers.<TrainingSessionEntity>lambdaQuery()
                .eq(TrainingSessionEntity::getOwnerUserId, ownerUserId)
                .isNotNull(TrainingSessionEntity::getCompletedAt)
                .orderByDesc(TrainingSessionEntity::getCompletedAt)
                .orderByDesc(TrainingSessionEntity::getId)
                .last("LIMIT 1"));

        String recommendedTrainingMode = resolveRecommendedTrainingMode(latestTraining, latestDiagnosis, recentAvgReactionTime, recentNegativeRisk);
        String primaryRiskLevel = resolveRiskLevel(recentNegativeRisk, pendingReviewCount).name();
        Map<String, Long> errorTotals = buildStudentErrorTotals(pairRows);
        List<String> focusTags = deriveFocusTags(recommendedTrainingMode, pendingReviewCount, recentNegativeRisk, recentAvgReactionTime);

        StudentAnalyticsSnapshotPayload payload = new StudentAnalyticsSnapshotPayload(
                user == null ? null : user.getDisplayName(),
                studentProfile == null ? null : studentProfile.getGradeName(),
                studentProfile == null ? null : studentProfile.getFrenchLevel(),
                latestDiagnosis == null ? null : latestDiagnosis.getId(),
                latestTraining == null ? null : latestTraining.getId(),
                primaryRiskLevel,
                recommendedTrainingMode,
                pendingReviewCount,
                highRiskPairCount,
                recentAccuracy,
                recentNegativeRisk,
                recentAvgReactionTime,
                lastActiveAt,
                pairRiskAggregates.stream()
                        .sorted(Comparator.comparingDouble(PairRiskAggregate::riskScore).reversed()
                                .thenComparing(PairRiskAggregate::incorrectCount, Comparator.reverseOrder()))
                        .limit(5)
                        .map(pair -> new StudentAnalyticsSnapshotPayload.StudentRiskPairPayload(
                                pair.lexicalPairId(),
                                pair.englishWord(),
                                pair.frenchWord(),
                                pair.lexicalPairType(),
                                pair.riskScore(),
                                pair.attemptCount(),
                                pair.incorrectCount()
                        ))
                        .toList(),
                toStudentErrorDistribution(errorTotals),
                focusTags
        );

        LearningProfileSnapshotEntity entity = learningProfileSnapshotMapper.selectOne(Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                .eq(LearningProfileSnapshotEntity::getScope, AnalyticsConstants.PROFILE_SCOPE_STUDENT)
                .eq(LearningProfileSnapshotEntity::getStudentUserId, ownerUserId));
        if (entity == null) {
            entity = new LearningProfileSnapshotEntity();
            entity.setScope(AnalyticsConstants.PROFILE_SCOPE_STUDENT);
            entity.setStudentUserId(ownerUserId);
        }
        entity.setTeachingClassId(null);
        entity.setTeacherUserId(resolveTeacherUserId(ownerUserId));
        entity.setLastDiagnosisSummaryId(latestDiagnosis == null ? null : latestDiagnosis.getId());
        entity.setLastTrainingSessionId(latestTraining == null ? null : latestTraining.getId());
        entity.setPrimaryRiskLevel(primaryRiskLevel);
        entity.setRecommendedTrainingMode(recommendedTrainingMode);
        entity.setPendingReviewCount(pendingReviewCount);
        entity.setHighRiskPairCount(highRiskPairCount);
        entity.setRecentAccuracy(decimal(recentAccuracy));
        entity.setRecentNegativeTransferRisk(decimal(recentNegativeRisk));
        entity.setRecentAvgReactionTimeMs(recentAvgReactionTime);
        entity.setLastActiveAt(lastActiveAt);
        entity.setSnapshotAt(LocalDateTime.now());
        entity.setVersion(1);
        entity.setSnapshotJson(analyticsJsonCodec.write(payload));
        if (entity.getId() == null) {
            learningProfileSnapshotMapper.insert(entity);
        } else {
            learningProfileSnapshotMapper.updateById(entity);
        }
    }

    private void rebuildClassSnapshot(Long classId) {
        TeachingClassEntity teachingClass = teachingClassMapper.selectById(classId);
        if (teachingClass == null) {
            return;
        }

        List<Long> studentIds = teachingClassService.listActiveStudentIds(classId);
        List<LearningProfileSnapshotEntity> studentSnapshots = studentIds.isEmpty()
                ? List.of()
                : learningProfileSnapshotMapper.selectList(Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                .eq(LearningProfileSnapshotEntity::getScope, AnalyticsConstants.PROFILE_SCOPE_STUDENT)
                .in(LearningProfileSnapshotEntity::getStudentUserId, studentIds));

        LocalDate startDate = LocalDate.now().minusDays(SNAPSHOT_RANGE_DAYS - 1L);
        List<ClassAnalyticsDailyAggregateEntity> rows = classAnalyticsDailyAggregateMapper.selectList(Wrappers.<ClassAnalyticsDailyAggregateEntity>lambdaQuery()
                .eq(ClassAnalyticsDailyAggregateEntity::getTeachingClassId, classId)
                .ge(ClassAnalyticsDailyAggregateEntity::getStatDate, startDate)
                .orderByAsc(ClassAnalyticsDailyAggregateEntity::getStatDate)
                .orderByAsc(ClassAnalyticsDailyAggregateEntity::getId));
        List<ClassAnalyticsDailyAggregateEntity> summaryRows = rows.stream()
                .filter(row -> AnalyticsConstants.AGGREGATION_LEVEL_SUMMARY.equalsIgnoreCase(row.getAggregationLevel()))
                .toList();
        List<ClassAnalyticsDailyAggregateEntity> pairRows = rows.stream()
                .filter(row -> AnalyticsConstants.AGGREGATION_LEVEL_PAIR.equalsIgnoreCase(row.getAggregationLevel()))
                .toList();

        long attempts = summaryRows.stream().mapToLong(row -> safeInt(row.getAttemptCount())).sum();
        long correct = summaryRows.stream().mapToLong(row -> safeInt(row.getCorrectCount())).sum();
        long totalReactionTime = summaryRows.stream().mapToLong(row -> safeLong(row.getTotalReactionTimeMs())).sum();
        long completionCount = summaryRows.stream().mapToLong(row -> safeInt(row.getCompletionCount())).sum();
        double recentAccuracy = attempts == 0 ? 0d : correct / (double) attempts;
        double recentNegativeRisk = completionCount == 0
                ? averageClassPairRisk(pairRows)
                : summaryRows.stream().mapToDouble(row -> safeDecimal(row.getNegativeTransferRiskSum())).sum() / completionCount;
        long recentAvgReactionTime = attempts == 0 ? 0L : Math.round(totalReactionTime / (double) attempts);
        LocalDateTime lastActiveAt = rows.stream()
                .map(ClassAnalyticsDailyAggregateEntity::getLastEventAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        int studentCount = studentIds.size();
        int activeStudentCount = (int) studentSnapshots.stream()
                .filter(snapshot -> snapshot.getLastActiveAt() != null && !snapshot.getLastActiveAt().toLocalDate().isBefore(startDate))
                .count();
        int highRiskStudentCount = (int) studentSnapshots.stream()
                .filter(snapshot -> "HIGH".equalsIgnoreCase(snapshot.getPrimaryRiskLevel())
                        || "CRITICAL".equalsIgnoreCase(snapshot.getPrimaryRiskLevel()))
                .count();
        String primaryRiskLevel = resolveClassRiskLevel(studentCount, highRiskStudentCount, recentNegativeRisk).name();
        Map<String, Long> errorTotals = buildClassErrorTotals(pairRows);
        List<PairRiskAggregate> pairRiskAggregates = buildClassPairRiskAggregates(pairRows);

        ClassAnalyticsSnapshotPayload payload = new ClassAnalyticsSnapshotPayload(
                teachingClass.getClassCode(),
                teachingClass.getClassName(),
                teachingClass.getGradeName(),
                studentCount,
                activeStudentCount,
                highRiskStudentCount,
                recentAccuracy,
                recentNegativeRisk,
                recentAvgReactionTime,
                primaryRiskLevel,
                lastActiveAt,
                buildClassRiskDistribution(studentSnapshots),
                buildClassModeFocus(studentSnapshots),
                pairRiskAggregates.stream()
                        .sorted(Comparator.comparingDouble(PairRiskAggregate::riskScore).reversed()
                                .thenComparing(PairRiskAggregate::incorrectCount, Comparator.reverseOrder()))
                        .limit(5)
                        .map(pair -> new ClassAnalyticsSnapshotPayload.ClassRiskPairPayload(
                                pair.lexicalPairId(),
                                pair.englishWord(),
                                pair.frenchWord(),
                                pair.lexicalPairType(),
                                pair.riskScore(),
                                pair.attemptCount(),
                                pair.incorrectCount()
                        ))
                        .toList(),
                toClassErrorDistribution(errorTotals)
        );

        LearningProfileSnapshotEntity entity = learningProfileSnapshotMapper.selectOne(Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                .eq(LearningProfileSnapshotEntity::getScope, AnalyticsConstants.PROFILE_SCOPE_CLASS)
                .eq(LearningProfileSnapshotEntity::getTeachingClassId, classId));
        if (entity == null) {
            entity = new LearningProfileSnapshotEntity();
            entity.setScope(AnalyticsConstants.PROFILE_SCOPE_CLASS);
            entity.setTeachingClassId(classId);
        }
        entity.setStudentUserId(null);
        entity.setTeacherUserId(teachingClass.getTeacherUserId());
        entity.setLastDiagnosisSummaryId(null);
        entity.setLastTrainingSessionId(null);
        entity.setPrimaryRiskLevel(primaryRiskLevel);
        entity.setRecommendedTrainingMode(payload.recommendedFocusModes().isEmpty()
                ? AnalyticsConstants.DIMENSION_ALL
                : payload.recommendedFocusModes().get(0).mode());
        entity.setPendingReviewCount(studentSnapshots.stream().mapToInt(snapshot -> safeInt(snapshot.getPendingReviewCount())).sum());
        entity.setHighRiskPairCount((int) pairRiskAggregates.stream().filter(pair -> pair.riskScore() >= 0.6d).count());
        entity.setRecentAccuracy(decimal(recentAccuracy));
        entity.setRecentNegativeTransferRisk(decimal(recentNegativeRisk));
        entity.setRecentAvgReactionTimeMs(recentAvgReactionTime);
        entity.setLastActiveAt(lastActiveAt);
        entity.setSnapshotAt(LocalDateTime.now());
        entity.setVersion(1);
        entity.setSnapshotJson(analyticsJsonCodec.write(payload));
        if (entity.getId() == null) {
            learningProfileSnapshotMapper.insert(entity);
        } else {
            learningProfileSnapshotMapper.updateById(entity);
        }
    }

    private void syncLegacyStudentProfile(Long ownerUserId) {
        StudentProfileEntity studentProfile = studentProfileMapper.selectOne(Wrappers.<StudentProfileEntity>lambdaQuery()
                .eq(StudentProfileEntity::getUserId, ownerUserId));
        if (studentProfile == null) {
            return;
        }
        LearningProfileSnapshotEntity snapshot = learningProfileSnapshotMapper.selectOne(Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                .eq(LearningProfileSnapshotEntity::getScope, AnalyticsConstants.PROFILE_SCOPE_STUDENT)
                .eq(LearningProfileSnapshotEntity::getStudentUserId, ownerUserId));
        if (snapshot == null) {
            return;
        }
        StudentAnalyticsSnapshotPayload payload = analyticsJsonCodec.read(snapshot.getSnapshotJson(), StudentAnalyticsSnapshotPayload.class);
        List<TrainingRiskWordSnapshot> riskWords = payload == null ? List.of() : payload.topRiskPairs().stream()
                .map(pair -> new TrainingRiskWordSnapshot(
                        pair.lexicalPairId(),
                        pair.englishWord(),
                        pair.frenchWord(),
                        null,
                        pair.lexicalPairType(),
                        "Recent analytics risk",
                        resolveRiskLevel(pair.riskScore(), 0).name(),
                        pair.riskScore() >= 0.7d ? "FALSE_FRIEND_CONFUSION" : null
                ))
                .toList();
        TrainingLearningProfileSnapshot legacySnapshot = new TrainingLearningProfileSnapshot(
                snapshot.getLastDiagnosisSummaryId(),
                snapshot.getLastTrainingSessionId(),
                snapshot.getRecommendedTrainingMode(),
                payload == null ? List.of() : payload.focusTags(),
                riskWords,
                safeInt(snapshot.getPendingReviewCount()),
                safeDecimal(snapshot.getRecentAccuracy()),
                safeLong(snapshot.getRecentAvgReactionTimeMs()),
                snapshot.getSnapshotAt()
        );
        studentProfile.setLearningProfileSnapshotJson(trainingJsonCodec.write(legacySnapshot));
        studentProfile.setLearningProfileUpdatedAt(snapshot.getSnapshotAt());
        studentProfileMapper.updateById(studentProfile);
    }

    private Map<Long, DiagnosisTemplateItemEntity> loadDiagnosisTemplateItemMap(List<Long> templateItemIds) {
        if (templateItemIds.isEmpty()) {
            return Map.of();
        }
        return diagnosisTemplateItemMapper.selectBatchIds(new LinkedHashSet<>(templateItemIds)).stream()
                .collect(Collectors.toMap(DiagnosisTemplateItemEntity::getId, item -> item));
    }

    private Map<Long, TrainingPlanItemEntity> loadTrainingPlanItemMap(List<Long> planItemIds) {
        if (planItemIds.isEmpty()) {
            return Map.of();
        }
        return trainingPlanItemMapper.selectBatchIds(new LinkedHashSet<>(planItemIds)).stream()
                .collect(Collectors.toMap(TrainingPlanItemEntity::getId, item -> item));
    }

    private Map<Long, LexicalPairEntity> loadLexicalPairMap(List<Long> pairIds) {
        if (pairIds.isEmpty()) {
            return Map.of();
        }
        return lexicalPairMapper.selectBatchIds(new LinkedHashSet<>(pairIds)).stream()
                .collect(Collectors.toMap(LexicalPairEntity::getId, pair -> pair));
    }

    private int pendingReviewCount(Long ownerUserId) {
        Long count = reviewScheduleMapper.selectCount(Wrappers.<ReviewScheduleEntity>lambdaQuery()
                .eq(ReviewScheduleEntity::getOwnerUserId, ownerUserId)
                .eq(ReviewScheduleEntity::getStatus, ReviewScheduleStatus.PENDING.name()));
        return count == null ? 0 : Math.toIntExact(count);
    }

    private Long resolveTeacherUserId(Long ownerUserId) {
        List<Long> classIds = teachingClassService.listActiveClassIdsByStudent(ownerUserId, LocalDateTime.now());
        if (classIds.isEmpty()) {
            return null;
        }
        TeachingClassEntity teachingClass = teachingClassMapper.selectById(classIds.get(0));
        return teachingClass == null ? null : teachingClass.getTeacherUserId();
    }

    private String resolveRecommendedTrainingMode(
            TrainingSessionEntity latestTraining,
            DiagnosisSummaryEntity latestDiagnosis,
            long recentAvgReactionTime,
            double recentNegativeRisk
    ) {
        if (latestTraining != null && latestTraining.getSummarySnapshotJson() != null && !latestTraining.getSummarySnapshotJson().isBlank()) {
            TrainingSessionSummarySnapshot summarySnapshot = trainingJsonCodec.readSummarySnapshot(latestTraining.getSummarySnapshotJson());
            if (summarySnapshot != null && summarySnapshot.nextRecommendedMode() != null) {
                return summarySnapshot.nextRecommendedMode();
            }
        }
        if (latestDiagnosis != null && latestDiagnosis.getContextSensitivity() != null && latestDiagnosis.getContextSensitivity().doubleValue() < 0.6d) {
            return TrainingMode.CONTEXT_FIX.name();
        }
        if (recentNegativeRisk >= 0.55d || (latestDiagnosis != null && latestDiagnosis.getNegativeTransferRisk() != null
                && latestDiagnosis.getNegativeTransferRisk().doubleValue() >= 0.55d)) {
            return TrainingMode.FALSE_FRIEND_DISCRIM.name();
        }
        if (recentAvgReactionTime >= 1200L) {
            return TrainingMode.SPEED_CHALLENGE.name();
        }
        return TrainingMode.COGNATE_BOOST.name();
    }

    private List<String> deriveFocusTags(String recommendedTrainingMode, int pendingReviewCount, double recentNegativeRisk, long recentAvgReactionTime) {
        List<String> tags = new ArrayList<>();
        if (pendingReviewCount > 0) {
            tags.add("spaced_review_backlog");
        }
        if (recentNegativeRisk >= 0.55d) {
            tags.add("negative_transfer_alert");
        }
        if (recentAvgReactionTime >= 1200L) {
            tags.add("processing_latency");
        }
        if (TrainingMode.FALSE_FRIEND_DISCRIM.name().equalsIgnoreCase(recommendedTrainingMode)) {
            tags.add("false_friend_control");
        } else if (TrainingMode.CONTEXT_FIX.name().equalsIgnoreCase(recommendedTrainingMode)) {
            tags.add("context_locking");
        } else if (TrainingMode.SPEED_CHALLENGE.name().equalsIgnoreCase(recommendedTrainingMode)) {
            tags.add("rapid_recognition");
        } else {
            tags.add("stability_consolidation");
        }
        return tags;
    }

    private Map<String, Long> buildStudentErrorTotals(List<AnalyticsDailyAggregateEntity> rows) {
        Map<String, Long> totals = initErrorTotals();
        for (AnalyticsDailyAggregateEntity row : rows) {
            totals.compute("FALSE_FRIEND_CONFUSION", (key, value) -> value + safeInt(row.getFalseFriendConfusionCount()));
            totals.compute("CONTEXT_IGNORED", (key, value) -> value + safeInt(row.getContextIgnoredCount()));
            totals.compute("OVER_TRANSFER", (key, value) -> value + safeInt(row.getOverTransferCount()));
            totals.compute("UNDER_TRANSFER", (key, value) -> value + safeInt(row.getUnderTransferCount()));
            totals.compute("ORTHOGRAPHIC_INTERFERENCE", (key, value) -> value + safeInt(row.getOrthographicInterferenceCount()));
            totals.compute("SEMANTIC_MISFIRE", (key, value) -> value + safeInt(row.getSemanticMisfireCount()));
        }
        return totals;
    }

    private Map<String, Long> buildClassErrorTotals(List<ClassAnalyticsDailyAggregateEntity> rows) {
        Map<String, Long> totals = initErrorTotals();
        for (ClassAnalyticsDailyAggregateEntity row : rows) {
            totals.compute("FALSE_FRIEND_CONFUSION", (key, value) -> value + safeInt(row.getFalseFriendConfusionCount()));
            totals.compute("CONTEXT_IGNORED", (key, value) -> value + safeInt(row.getContextIgnoredCount()));
            totals.compute("OVER_TRANSFER", (key, value) -> value + safeInt(row.getOverTransferCount()));
            totals.compute("UNDER_TRANSFER", (key, value) -> value + safeInt(row.getUnderTransferCount()));
            totals.compute("ORTHOGRAPHIC_INTERFERENCE", (key, value) -> value + safeInt(row.getOrthographicInterferenceCount()));
            totals.compute("SEMANTIC_MISFIRE", (key, value) -> value + safeInt(row.getSemanticMisfireCount()));
        }
        return totals;
    }

    private Map<String, Long> initErrorTotals() {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (String errorType : AnalyticsConstants.ERROR_TYPES) {
            totals.put(errorType, 0L);
        }
        return totals;
    }

    private List<StudentAnalyticsSnapshotPayload.ErrorDistributionPayload> toStudentErrorDistribution(Map<String, Long> totals) {
        long total = totals.values().stream().mapToLong(Long::longValue).sum();
        return totals.entrySet().stream()
                .map(entry -> new StudentAnalyticsSnapshotPayload.ErrorDistributionPayload(
                        entry.getKey(),
                        entry.getValue(),
                        total == 0 ? 0d : entry.getValue() / (double) total
                ))
                .toList();
    }

    private List<ClassAnalyticsSnapshotPayload.ErrorDistributionPayload> toClassErrorDistribution(Map<String, Long> totals) {
        long total = totals.values().stream().mapToLong(Long::longValue).sum();
        return totals.entrySet().stream()
                .map(entry -> new ClassAnalyticsSnapshotPayload.ErrorDistributionPayload(
                        entry.getKey(),
                        entry.getValue(),
                        total == 0 ? 0d : entry.getValue() / (double) total
                ))
                .toList();
    }

    private List<PairRiskAggregate> buildStudentPairRiskAggregates(List<AnalyticsDailyAggregateEntity> rows) {
        Map<Long, PairRiskAccumulator> accumulators = new LinkedHashMap<>();
        Map<Long, LexicalPairEntity> pairMap = loadLexicalPairMap(rows.stream()
                .map(AnalyticsDailyAggregateEntity::getLexicalPairId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList());
        for (AnalyticsDailyAggregateEntity row : rows) {
            if (row.getLexicalPairId() == null || row.getLexicalPairId() <= 0) {
                continue;
            }
            LexicalPairEntity pair = pairMap.get(row.getLexicalPairId());
            accumulators.computeIfAbsent(row.getLexicalPairId(), ignored -> new PairRiskAccumulator(
                    row.getLexicalPairId(),
                    pair == null ? null : pair.getEnglishWord(),
                    pair == null ? null : pair.getFrenchWord(),
                    normalizePairType(pair == null ? row.getLexicalPairType() : pair.getLexicalPairType())
            )).add(
                    safeInt(row.getAttemptCount()),
                    safeInt(row.getIncorrectCount()),
                    safeDecimal(row.getNegativeTransferRiskSum()),
                    safeInt(row.getHighRiskCount())
            );
        }
        return accumulators.values().stream().map(PairRiskAccumulator::toAggregate).toList();
    }

    private List<PairRiskAggregate> buildClassPairRiskAggregates(List<ClassAnalyticsDailyAggregateEntity> rows) {
        Map<Long, PairRiskAccumulator> accumulators = new LinkedHashMap<>();
        Map<Long, LexicalPairEntity> pairMap = loadLexicalPairMap(rows.stream()
                .map(ClassAnalyticsDailyAggregateEntity::getLexicalPairId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList());
        for (ClassAnalyticsDailyAggregateEntity row : rows) {
            if (row.getLexicalPairId() == null || row.getLexicalPairId() <= 0) {
                continue;
            }
            LexicalPairEntity pair = pairMap.get(row.getLexicalPairId());
            accumulators.computeIfAbsent(row.getLexicalPairId(), ignored -> new PairRiskAccumulator(
                    row.getLexicalPairId(),
                    pair == null ? null : pair.getEnglishWord(),
                    pair == null ? null : pair.getFrenchWord(),
                    normalizePairType(pair == null ? row.getLexicalPairType() : pair.getLexicalPairType())
            )).add(
                    safeInt(row.getAttemptCount()),
                    safeInt(row.getIncorrectCount()),
                    safeDecimal(row.getNegativeTransferRiskSum()),
                    safeInt(row.getHighRiskCount())
            );
        }
        return accumulators.values().stream().map(PairRiskAccumulator::toAggregate).toList();
    }

    private List<ClassAnalyticsSnapshotPayload.RiskBucketPayload> buildClassRiskDistribution(List<LearningProfileSnapshotEntity> studentSnapshots) {
        return List.of(
                new ClassAnalyticsSnapshotPayload.RiskBucketPayload("LOW", countByRisk(studentSnapshots, "LOW")),
                new ClassAnalyticsSnapshotPayload.RiskBucketPayload("MEDIUM", countByRisk(studentSnapshots, "MEDIUM")),
                new ClassAnalyticsSnapshotPayload.RiskBucketPayload("HIGH", countByRisk(studentSnapshots, "HIGH")),
                new ClassAnalyticsSnapshotPayload.RiskBucketPayload("CRITICAL", countByRisk(studentSnapshots, "CRITICAL"))
        );
    }

    private List<ClassAnalyticsSnapshotPayload.ModeFocusPayload> buildClassModeFocus(List<LearningProfileSnapshotEntity> studentSnapshots) {
        return studentSnapshots.stream()
                .filter(snapshot -> snapshot.getRecommendedTrainingMode() != null && !snapshot.getRecommendedTrainingMode().isBlank())
                .collect(Collectors.groupingBy(LearningProfileSnapshotEntity::getRecommendedTrainingMode, LinkedHashMap::new, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(entry -> new ClassAnalyticsSnapshotPayload.ModeFocusPayload(entry.getKey(), entry.getValue()))
                .toList();
    }

    private long countByRisk(List<LearningProfileSnapshotEntity> studentSnapshots, String riskLevel) {
        return studentSnapshots.stream()
                .filter(snapshot -> riskLevel.equalsIgnoreCase(snapshot.getPrimaryRiskLevel()))
                .count();
    }

    private double averagePairRisk(List<AnalyticsDailyAggregateEntity> rows) {
        long attempts = rows.stream().mapToLong(row -> safeInt(row.getAttemptCount())).sum();
        if (attempts == 0) {
            return 0d;
        }
        return rows.stream().mapToDouble(row -> safeDecimal(row.getNegativeTransferRiskSum())).sum() / attempts;
    }

    private double averageClassPairRisk(List<ClassAnalyticsDailyAggregateEntity> rows) {
        long attempts = rows.stream().mapToLong(row -> safeInt(row.getAttemptCount())).sum();
        if (attempts == 0) {
            return 0d;
        }
        return rows.stream().mapToDouble(row -> safeDecimal(row.getNegativeTransferRiskSum())).sum() / attempts;
    }

    private String normalizePairType(String rawValue) {
        if (rawValue == null || rawValue.isBlank() || AnalyticsConstants.DIMENSION_ALL.equalsIgnoreCase(rawValue)) {
            return AnalyticsConstants.DIMENSION_ALL;
        }
        return LexicalPairType.fromCode(rawValue).name();
    }

    private String normalizeTrainingMode(String rawValue) {
        if (rawValue == null || rawValue.isBlank() || AnalyticsConstants.DIMENSION_ALL.equalsIgnoreCase(rawValue)) {
            return AnalyticsConstants.DIMENSION_ALL;
        }
        return TrainingMode.fromCode(rawValue).name();
    }

    private String normalizeContextLevel(String rawValue) {
        if (rawValue == null || rawValue.isBlank() || AnalyticsConstants.DIMENSION_ALL.equalsIgnoreCase(rawValue)) {
            return AnalyticsConstants.DIMENSION_ALL;
        }
        return ContextSupportLevel.fromCode(rawValue).name();
    }

    private RiskLevel resolveRiskLevel(double recentNegativeRisk, int pendingReviewCount) {
        if (pendingReviewCount >= 8 || recentNegativeRisk >= 0.75d) {
            return RiskLevel.CRITICAL;
        }
        if (pendingReviewCount >= 4 || recentNegativeRisk >= 0.55d) {
            return RiskLevel.HIGH;
        }
        if (pendingReviewCount >= 1 || recentNegativeRisk >= 0.30d) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private RiskLevel resolveClassRiskLevel(int studentCount, int highRiskStudentCount, double recentNegativeRisk) {
        if (studentCount > 0 && highRiskStudentCount >= Math.ceil(studentCount * 0.4d)) {
            return RiskLevel.CRITICAL;
        }
        if (studentCount > 0 && highRiskStudentCount >= Math.ceil(studentCount * 0.2d)) {
            return RiskLevel.HIGH;
        }
        if (recentNegativeRisk >= 0.30d) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double safeDecimal(BigDecimal value) {
        return value == null ? 0d : value.doubleValue();
    }

    private record RebuildTask(
            Long ownerUserId,
            LocalDateTime eventAt,
            String sourceType
    ) {
    }

    private record AggregateKey(
            String sourceType,
            String aggregationLevel,
            Long lexicalPairId,
            String lexicalPairType,
            String trainingMode,
            String contextSupportLevel
    ) {
    }

    private static final class AggregateAccumulator {

        private final AggregateKey key;
        private int attemptCount;
        private int correctCount;
        private int incorrectCount;
        private long totalReactionTimeMs;
        private long totalHesitationTimeMs;
        private double positiveTransferScoreSum;
        private double negativeTransferRiskSum;
        private double contextSensitivitySum;
        private double semanticDiscriminationSum;
        private int highRiskCount;
        private int pendingReviewCount;
        private int completionCount;
        private int falseFriendConfusionCount;
        private int contextIgnoredCount;
        private int overTransferCount;
        private int underTransferCount;
        private int orthographicInterferenceCount;
        private int semanticMisfireCount;
        private LocalDateTime lastEventAt;

        private AggregateAccumulator(AggregateKey key) {
            this.key = key;
        }

        private void addDiagnosisSummary(
                DiagnosisSummaryEntity summary,
                List<DiagnosisItemResultEntity> itemResults,
                int pendingReviewCount,
                int highRiskCount
        ) {
            attemptCount += itemResults.size();
            correctCount += (int) itemResults.stream().filter(item -> Boolean.TRUE.equals(item.getIsCorrect())).count();
            incorrectCount += (int) itemResults.stream().filter(item -> Boolean.FALSE.equals(item.getIsCorrect())).count();
            totalReactionTimeMs += itemResults.stream().mapToLong(item -> item.getReactionTimeMs() == null ? 0 : item.getReactionTimeMs()).sum();
            totalHesitationTimeMs += itemResults.stream().mapToLong(item -> item.getHesitationTimeMs() == null ? 0 : item.getHesitationTimeMs()).sum();
            positiveTransferScoreSum += summary.getPositiveTransferScore() == null ? 0d : summary.getPositiveTransferScore().doubleValue();
            negativeTransferRiskSum += summary.getNegativeTransferRisk() == null ? 0d : summary.getNegativeTransferRisk().doubleValue();
            contextSensitivitySum += summary.getContextSensitivity() == null ? 0d : summary.getContextSensitivity().doubleValue();
            semanticDiscriminationSum += summary.getSemanticDiscrimination() == null ? 0d : summary.getSemanticDiscrimination().doubleValue();
            this.highRiskCount += highRiskCount;
            this.pendingReviewCount += pendingReviewCount;
            completionCount += 1;
            itemResults.forEach(item -> incrementErrorType(item.getDetectedErrorType()));
            updateLastEvent(summary.getGeneratedAt());
        }

        private void addDiagnosisItem(DiagnosisItemResultEntity itemResult) {
            attemptCount += 1;
            if (Boolean.TRUE.equals(itemResult.getIsCorrect())) {
                correctCount += 1;
            }
            if (Boolean.FALSE.equals(itemResult.getIsCorrect())) {
                incorrectCount += 1;
            }
            totalReactionTimeMs += itemResult.getReactionTimeMs() == null ? 0 : itemResult.getReactionTimeMs();
            totalHesitationTimeMs += itemResult.getHesitationTimeMs() == null ? 0 : itemResult.getHesitationTimeMs();
            positiveTransferScoreSum += itemResult.getItemScore() == null ? 0d : itemResult.getItemScore().doubleValue();
            negativeTransferRiskSum += itemResult.getTransferRiskScore() == null ? 0d : itemResult.getTransferRiskScore().doubleValue();
            highRiskCount += itemResult.getTransferRiskScore() != null && itemResult.getTransferRiskScore().doubleValue() >= 0.75d ? 1 : 0;
            incrementErrorType(itemResult.getDetectedErrorType());
            updateLastEvent(itemResult.getSubmittedAt());
        }

        private void addTrainingSummary(
                TrainingSessionEntity session,
                TrainingSessionSummarySnapshot summarySnapshot,
                List<TrainingItemResultEntity> itemResults,
                int pendingReviewCount
        ) {
            int attempts = itemResults.size();
            int incorrect = (int) itemResults.stream().filter(item -> Boolean.FALSE.equals(item.getIsCorrect())).count();
            attemptCount += attempts;
            correctCount += (int) itemResults.stream().filter(item -> Boolean.TRUE.equals(item.getIsCorrect())).count();
            incorrectCount += incorrect;
            totalReactionTimeMs += itemResults.stream().mapToLong(item -> item.getReactionTimeMs() == null ? 0 : item.getReactionTimeMs()).sum();
            totalHesitationTimeMs += itemResults.stream().mapToLong(item -> item.getHesitationTimeMs() == null ? 0 : item.getHesitationTimeMs()).sum();
            negativeTransferRiskSum += attempts == 0 ? 0d : incorrect / (double) attempts;
            highRiskCount += summarySnapshot == null ? 0 : summarySnapshot.riskWordsToReview().size();
            this.pendingReviewCount += pendingReviewCount;
            completionCount += 1;
            itemResults.forEach(item -> incrementErrorType(item.getDetectedErrorType()));
            updateLastEvent(session.getCompletedAt());
        }

        private void addTrainingItem(TrainingItemResultEntity itemResult) {
            attemptCount += 1;
            if (Boolean.TRUE.equals(itemResult.getIsCorrect())) {
                correctCount += 1;
            }
            if (Boolean.FALSE.equals(itemResult.getIsCorrect())) {
                incorrectCount += 1;
            }
            totalReactionTimeMs += itemResult.getReactionTimeMs() == null ? 0 : itemResult.getReactionTimeMs();
            totalHesitationTimeMs += itemResult.getHesitationTimeMs() == null ? 0 : itemResult.getHesitationTimeMs();
            negativeTransferRiskSum += Boolean.TRUE.equals(itemResult.getIsCorrect()) ? 0d : 1d;
            highRiskCount += Boolean.TRUE.equals(itemResult.getReviewRequired()) ? 1 : 0;
            incrementErrorType(itemResult.getDetectedErrorType());
            updateLastEvent(itemResult.getSubmittedAt());
        }

        private void mergeStudentRow(AnalyticsDailyAggregateEntity row) {
            attemptCount += row.getAttemptCount() == null ? 0 : row.getAttemptCount();
            correctCount += row.getCorrectCount() == null ? 0 : row.getCorrectCount();
            incorrectCount += row.getIncorrectCount() == null ? 0 : row.getIncorrectCount();
            totalReactionTimeMs += row.getTotalReactionTimeMs() == null ? 0 : row.getTotalReactionTimeMs();
            totalHesitationTimeMs += row.getTotalHesitationTimeMs() == null ? 0 : row.getTotalHesitationTimeMs();
            positiveTransferScoreSum += row.getPositiveTransferScoreSum() == null ? 0d : row.getPositiveTransferScoreSum().doubleValue();
            negativeTransferRiskSum += row.getNegativeTransferRiskSum() == null ? 0d : row.getNegativeTransferRiskSum().doubleValue();
            contextSensitivitySum += row.getContextSensitivitySum() == null ? 0d : row.getContextSensitivitySum().doubleValue();
            semanticDiscriminationSum += row.getSemanticDiscriminationSum() == null ? 0d : row.getSemanticDiscriminationSum().doubleValue();
            highRiskCount += row.getHighRiskCount() == null ? 0 : row.getHighRiskCount();
            pendingReviewCount += row.getPendingReviewCount() == null ? 0 : row.getPendingReviewCount();
            completionCount += row.getCompletionCount() == null ? 0 : row.getCompletionCount();
            falseFriendConfusionCount += row.getFalseFriendConfusionCount() == null ? 0 : row.getFalseFriendConfusionCount();
            contextIgnoredCount += row.getContextIgnoredCount() == null ? 0 : row.getContextIgnoredCount();
            overTransferCount += row.getOverTransferCount() == null ? 0 : row.getOverTransferCount();
            underTransferCount += row.getUnderTransferCount() == null ? 0 : row.getUnderTransferCount();
            orthographicInterferenceCount += row.getOrthographicInterferenceCount() == null ? 0 : row.getOrthographicInterferenceCount();
            semanticMisfireCount += row.getSemanticMisfireCount() == null ? 0 : row.getSemanticMisfireCount();
            updateLastEvent(row.getLastEventAt());
        }

        private AnalyticsDailyAggregateEntity toStudentEntity(Long ownerUserId, LocalDate statDate) {
            AnalyticsDailyAggregateEntity entity = new AnalyticsDailyAggregateEntity();
            entity.setOwnerUserId(ownerUserId);
            entity.setStatDate(statDate);
            entity.setWeekStartDate(statDate.with(DayOfWeek.MONDAY));
            entity.setSourceType(key.sourceType());
            entity.setAggregationLevel(key.aggregationLevel());
            entity.setLexicalPairId(key.lexicalPairId());
            entity.setLexicalPairType(key.lexicalPairType());
            entity.setTrainingMode(key.trainingMode());
            entity.setContextSupportLevel(key.contextSupportLevel());
            entity.setAttemptCount(attemptCount);
            entity.setCorrectCount(correctCount);
            entity.setIncorrectCount(incorrectCount);
            entity.setTotalReactionTimeMs(totalReactionTimeMs);
            entity.setTotalHesitationTimeMs(totalHesitationTimeMs);
            entity.setPositiveTransferScoreSum(decimalValue(positiveTransferScoreSum));
            entity.setNegativeTransferRiskSum(decimalValue(negativeTransferRiskSum));
            entity.setContextSensitivitySum(decimalValue(contextSensitivitySum));
            entity.setSemanticDiscriminationSum(decimalValue(semanticDiscriminationSum));
            entity.setHighRiskCount(highRiskCount);
            entity.setPendingReviewCount(pendingReviewCount);
            entity.setCompletionCount(completionCount);
            entity.setFalseFriendConfusionCount(falseFriendConfusionCount);
            entity.setContextIgnoredCount(contextIgnoredCount);
            entity.setOverTransferCount(overTransferCount);
            entity.setUnderTransferCount(underTransferCount);
            entity.setOrthographicInterferenceCount(orthographicInterferenceCount);
            entity.setSemanticMisfireCount(semanticMisfireCount);
            entity.setLastEventAt(lastEventAt);
            return entity;
        }

        private ClassAnalyticsDailyAggregateEntity toClassEntity(Long classId, LocalDate statDate) {
            ClassAnalyticsDailyAggregateEntity entity = new ClassAnalyticsDailyAggregateEntity();
            entity.setTeachingClassId(classId);
            entity.setStatDate(statDate);
            entity.setWeekStartDate(statDate.with(DayOfWeek.MONDAY));
            entity.setSourceType(key.sourceType());
            entity.setAggregationLevel(key.aggregationLevel());
            entity.setLexicalPairId(key.lexicalPairId());
            entity.setLexicalPairType(key.lexicalPairType());
            entity.setTrainingMode(key.trainingMode());
            entity.setContextSupportLevel(key.contextSupportLevel());
            entity.setAttemptCount(attemptCount);
            entity.setCorrectCount(correctCount);
            entity.setIncorrectCount(incorrectCount);
            entity.setTotalReactionTimeMs(totalReactionTimeMs);
            entity.setTotalHesitationTimeMs(totalHesitationTimeMs);
            entity.setPositiveTransferScoreSum(decimalValue(positiveTransferScoreSum));
            entity.setNegativeTransferRiskSum(decimalValue(negativeTransferRiskSum));
            entity.setContextSensitivitySum(decimalValue(contextSensitivitySum));
            entity.setSemanticDiscriminationSum(decimalValue(semanticDiscriminationSum));
            entity.setHighRiskCount(highRiskCount);
            entity.setPendingReviewCount(pendingReviewCount);
            entity.setCompletionCount(completionCount);
            entity.setFalseFriendConfusionCount(falseFriendConfusionCount);
            entity.setContextIgnoredCount(contextIgnoredCount);
            entity.setOverTransferCount(overTransferCount);
            entity.setUnderTransferCount(underTransferCount);
            entity.setOrthographicInterferenceCount(orthographicInterferenceCount);
            entity.setSemanticMisfireCount(semanticMisfireCount);
            entity.setLastEventAt(lastEventAt);
            return entity;
        }

        private void incrementErrorType(String errorType) {
            if (errorType == null || errorType.isBlank()) {
                return;
            }
            switch (errorType.toUpperCase()) {
                case "FALSE_FRIEND_CONFUSION" -> falseFriendConfusionCount += 1;
                case "CONTEXT_IGNORED" -> contextIgnoredCount += 1;
                case "OVER_TRANSFER" -> overTransferCount += 1;
                case "UNDER_TRANSFER" -> underTransferCount += 1;
                case "ORTHOGRAPHIC_INTERFERENCE" -> orthographicInterferenceCount += 1;
                case "SEMANTIC_MISFIRE" -> semanticMisfireCount += 1;
                default -> {
                }
            }
        }

        private void updateLastEvent(LocalDateTime eventAt) {
            if (eventAt == null) {
                return;
            }
            if (lastEventAt == null || eventAt.isAfter(lastEventAt)) {
                lastEventAt = eventAt;
            }
        }

        private BigDecimal decimalValue(double value) {
            return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
        }
    }

    private record PairRiskAggregate(
            Long lexicalPairId,
            String englishWord,
            String frenchWord,
            String lexicalPairType,
            double riskScore,
            int attemptCount,
            int incorrectCount
    ) {
    }

    private static final class PairRiskAccumulator {

        private final Long lexicalPairId;
        private final String englishWord;
        private final String frenchWord;
        private final String lexicalPairType;
        private int attemptCount;
        private int incorrectCount;
        private double riskSum;
        private int highRiskCount;

        private PairRiskAccumulator(Long lexicalPairId, String englishWord, String frenchWord, String lexicalPairType) {
            this.lexicalPairId = lexicalPairId;
            this.englishWord = englishWord;
            this.frenchWord = frenchWord;
            this.lexicalPairType = lexicalPairType;
        }

        private void add(int attempts, int incorrect, double risk, int highRiskCount) {
            attemptCount += attempts;
            incorrectCount += incorrect;
            riskSum += risk;
            this.highRiskCount += highRiskCount;
        }

        private PairRiskAggregate toAggregate() {
            double averageRisk = attemptCount == 0 ? 0d : riskSum / attemptCount;
            double incorrectRate = attemptCount == 0 ? 0d : incorrectCount / (double) attemptCount;
            double score = Math.min(1d, Math.max(averageRisk, incorrectRate) + Math.min(0.2d, highRiskCount * 0.03d));
            return new PairRiskAggregate(lexicalPairId, englishWord, frenchWord, lexicalPairType, score, attemptCount, incorrectCount);
        }
    }
}
