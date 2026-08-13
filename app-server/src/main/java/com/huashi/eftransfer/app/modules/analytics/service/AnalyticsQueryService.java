package com.huashi.eftransfer.app.modules.analytics.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.achievement.service.AchievementService;
import com.huashi.eftransfer.app.modules.analytics.entity.AnalyticsDailyAggregateEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.ClassAnalyticsDailyAggregateEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.LearningProfileSnapshotEntity;
import com.huashi.eftransfer.app.modules.analytics.entity.TeachingClassEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.AnalyticsDailyAggregateMapper;
import com.huashi.eftransfer.app.modules.analytics.mapper.ClassAnalyticsDailyAggregateMapper;
import com.huashi.eftransfer.app.modules.analytics.mapper.LearningProfileSnapshotMapper;
import com.huashi.eftransfer.app.modules.analytics.support.AnalyticsConstants;
import com.huashi.eftransfer.app.modules.analytics.support.AnalyticsJsonCodec;
import com.huashi.eftransfer.app.modules.analytics.support.ClassAnalyticsSnapshotPayload;
import com.huashi.eftransfer.app.modules.analytics.support.StudentAnalyticsSnapshotPayload;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsCardVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsContextPerformanceVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsErrorDistributionVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsHeatmapCellVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsHeatmapMetaVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsHeatmapVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsRadarMetricVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsRiskBucketVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsRiskPairVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsScatterPointVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsScatterVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsSeriesVO;
import com.huashi.eftransfer.app.modules.analytics.vo.AnalyticsTrendVO;
import com.huashi.eftransfer.app.modules.analytics.vo.ClassAnalyticsOverviewVO;
import com.huashi.eftransfer.app.modules.analytics.vo.ClassCompletionByModeVO;
import com.huashi.eftransfer.app.modules.analytics.vo.ClassCompletionRateVO;
import com.huashi.eftransfer.app.modules.analytics.vo.StudentAnalyticsDetailVO;
import com.huashi.eftransfer.app.modules.analytics.vo.StudentAnalyticsOverviewVO;
import com.huashi.eftransfer.app.modules.analytics.vo.StudentProfileSummaryVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeacherStudentDetailVO;
import com.huashi.eftransfer.app.modules.analytics.vo.TeachingClassSummaryVO;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairMapper;
import com.huashi.eftransfer.app.modules.user.entity.StudentProfileEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.app.modules.user.vo.StudentLearningGoalVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.ContextSupportLevel;
import com.huashi.eftransfer.shared.enums.LexicalPairType;
import com.huashi.eftransfer.shared.enums.TrainingMode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AnalyticsQueryService {

    private static final Map<String, String> ERROR_LABELS = Map.of(
            "FALSE_FRIEND_CONFUSION", "假朋友混淆",
            "CONTEXT_IGNORED", "忽略语境",
            "OVER_TRANSFER", "过度迁移",
            "UNDER_TRANSFER", "迁移不足",
            "ORTHOGRAPHIC_INTERFERENCE", "形近干扰",
            "SEMANTIC_MISFIRE", "语义误判"
    );

    private static final Map<String, String> PAIR_TYPE_LABELS = Map.of(
            LexicalPairType.COGNATE.name(), "同源词",
            LexicalPairType.FALSE_FRIEND.name(), "假朋友词",
            LexicalPairType.PARTIAL_COGNATE.name(), "部分同源",
            LexicalPairType.ORTHOGRAPHIC_SIMILAR.name(), "形近词"
    );

    private static final Map<String, String> MODE_LABELS = Map.of(
            TrainingMode.COGNATE_BOOST.name(), "同源强化",
            TrainingMode.FALSE_FRIEND_DISCRIM.name(), "假朋友辨析",
            TrainingMode.CONTEXT_FIX.name(), "语境修复",
            TrainingMode.SPEED_CHALLENGE.name(), "快速识别",
            AnalyticsConstants.DIMENSION_ALL, "全部模式"
    );

    private static final Map<String, String> CONTEXT_LABELS = Map.of(
            ContextSupportLevel.LOW.name(), "低语境支持",
            ContextSupportLevel.MEDIUM.name(), "中语境支持",
            ContextSupportLevel.HIGH.name(), "高语境支持",
            AnalyticsConstants.DIMENSION_ALL, "全部语境"
    );

    private static final List<String> PAIR_TYPE_ORDER = List.of(
            LexicalPairType.COGNATE.name(),
            LexicalPairType.FALSE_FRIEND.name(),
            LexicalPairType.PARTIAL_COGNATE.name(),
            LexicalPairType.ORTHOGRAPHIC_SIMILAR.name()
    );

    private static final List<String> CONTEXT_ORDER = List.of(
            ContextSupportLevel.LOW.name(),
            ContextSupportLevel.MEDIUM.name(),
            ContextSupportLevel.HIGH.name()
    );

    private static final List<RiskBucketDefinition> RISK_BUCKETS = List.of(
            new RiskBucketDefinition(0.0d, 0.2d),
            new RiskBucketDefinition(0.2d, 0.4d),
            new RiskBucketDefinition(0.4d, 0.6d),
            new RiskBucketDefinition(0.6d, 0.8d),
            new RiskBucketDefinition(0.8d, 1.0d)
    );

    private final AnalyticsDailyAggregateMapper analyticsDailyAggregateMapper;
    private final ClassAnalyticsDailyAggregateMapper classAnalyticsDailyAggregateMapper;
    private final LearningProfileSnapshotMapper learningProfileSnapshotMapper;
    private final TeachingClassService teachingClassService;
    private final UserMapper userMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final LexicalPairMapper lexicalPairMapper;
    private final AnalyticsJsonCodec analyticsJsonCodec;
    private final AchievementService achievementService;

    public AnalyticsQueryService(
            AnalyticsDailyAggregateMapper analyticsDailyAggregateMapper,
            ClassAnalyticsDailyAggregateMapper classAnalyticsDailyAggregateMapper,
            LearningProfileSnapshotMapper learningProfileSnapshotMapper,
            TeachingClassService teachingClassService,
            UserMapper userMapper,
            StudentProfileMapper studentProfileMapper,
            LexicalPairMapper lexicalPairMapper,
            AnalyticsJsonCodec analyticsJsonCodec,
            AchievementService achievementService
    ) {
        this.analyticsDailyAggregateMapper = analyticsDailyAggregateMapper;
        this.classAnalyticsDailyAggregateMapper = classAnalyticsDailyAggregateMapper;
        this.learningProfileSnapshotMapper = learningProfileSnapshotMapper;
        this.teachingClassService = teachingClassService;
        this.userMapper = userMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.lexicalPairMapper = lexicalPairMapper;
        this.analyticsJsonCodec = analyticsJsonCodec;
        this.achievementService = achievementService;
    }

    public StudentAnalyticsOverviewVO getCurrentStudentOverview() {
        return buildStudentOverview(requireCurrentUserId());
    }

    public StudentLearningGoalVO getCurrentStudentLearningGoal() {
        Long studentUserId = requireCurrentUserId();
        List<AggregateSlice> summaryRows = loadStudentSlices(studentUserId, parseWindow("30d"), AnalyticsConstants.AGGREGATION_LEVEL_SUMMARY);
        return buildStudentLearningGoal(loadStudentProfile(studentUserId), summaryRows);
    }

    public AnalyticsTrendVO getCurrentStudentTrends(String range, String bucket) {
        Long studentUserId = requireCurrentUserId();
        AnalyticsWindow window = parseWindow(range);
        return buildTrend(loadStudentSlices(studentUserId, window, AnalyticsConstants.AGGREGATION_LEVEL_SUMMARY), parseBucket(bucket), window);
    }

    public AnalyticsHeatmapVO getCurrentStudentTransferHeatmap(String range, String trainingMode, String contextSupportLevel) {
        Long studentUserId = requireCurrentUserId();
        AnalyticsWindow window = parseWindow(range);
        return buildHeatmap(
                loadStudentSlices(studentUserId, window, AnalyticsConstants.AGGREGATION_LEVEL_PAIR),
                normalizeOptionalTrainingMode(trainingMode),
                normalizeOptionalContextLevel(contextSupportLevel),
                window
        );
    }

    public AnalyticsScatterVO getCurrentStudentScatter(String range) {
        Long studentUserId = requireCurrentUserId();
        AnalyticsWindow window = parseWindow(range);
        return buildScatter(loadStudentSlices(studentUserId, window, AnalyticsConstants.AGGREGATION_LEVEL_PAIR));
    }

    public List<AnalyticsRiskPairVO> getCurrentStudentHighRiskPairs(String range, int limit) {
        Long studentUserId = requireCurrentUserId();
        AnalyticsWindow window = parseWindow(range);
        return buildHighRiskPairs(loadStudentSlices(studentUserId, window, AnalyticsConstants.AGGREGATION_LEVEL_PAIR), normalizeLimit(limit));
    }

    public List<AnalyticsErrorDistributionVO> getCurrentStudentErrorDistribution(String range) {
        Long studentUserId = requireCurrentUserId();
        AnalyticsWindow window = parseWindow(range);
        return buildErrorDistribution(loadStudentSlices(studentUserId, window, AnalyticsConstants.AGGREGATION_LEVEL_PAIR));
    }

    public StudentAnalyticsDetailVO getStudentDetail(Long studentUserId) {
        return buildStudentDetail(studentUserId);
    }

    public List<TeachingClassSummaryVO> listAccessibleClasses() {
        return teachingClassService.listAccessibleClasses().stream()
                .map(teachingClass -> new TeachingClassSummaryVO(
                        teachingClass.getId(),
                        teachingClass.getClassCode(),
                        teachingClass.getClassName(),
                        teachingClass.getGradeName(),
                        teachingClassService.countActiveStudents(teachingClass.getId())
                ))
                .toList();
    }

    public ClassAnalyticsOverviewVO getClassOverview(Long classId, String range) {
        TeachingClassEntity teachingClass = teachingClassService.requireAccessibleClass(classId);
        AnalyticsWindow window = parseWindow(range);
        return buildClassOverview(teachingClass, window);
    }

    public List<AnalyticsRiskBucketVO> getClassRiskDistribution(Long classId) {
        teachingClassService.requireAccessibleClass(classId);
        List<Long> studentIds = teachingClassService.listActiveStudentIds(classId);
        Map<Long, LearningProfileSnapshotEntity> snapshotMap = loadStudentSnapshotsByIds(studentIds).stream()
                .collect(Collectors.toMap(LearningProfileSnapshotEntity::getStudentUserId, Function.identity()));
        List<Double> riskValues = studentIds.stream()
                .map(studentId -> {
                    LearningProfileSnapshotEntity snapshot = snapshotMap.get(studentId);
                    return snapshot == null ? 0d : decimal(snapshot.getRecentNegativeTransferRisk());
                })
                .toList();
        return buildRiskDistribution(riskValues);
    }

    public AnalyticsHeatmapVO getClassTransferHeatmap(Long classId, String range, String trainingMode, String contextSupportLevel) {
        teachingClassService.requireAccessibleClass(classId);
        AnalyticsWindow window = parseWindow(range);
        return buildHeatmap(
                loadClassSlices(classId, window, AnalyticsConstants.AGGREGATION_LEVEL_PAIR),
                normalizeOptionalTrainingMode(trainingMode),
                normalizeOptionalContextLevel(contextSupportLevel),
                window
        );
    }

    public List<AnalyticsErrorDistributionVO> getClassErrorDistribution(Long classId, String range) {
        teachingClassService.requireAccessibleClass(classId);
        AnalyticsWindow window = parseWindow(range);
        return buildErrorDistribution(loadClassSlices(classId, window, AnalyticsConstants.AGGREGATION_LEVEL_PAIR));
    }

    public ClassCompletionRateVO getClassCompletionRate(Long classId, String range, String bucket) {
        teachingClassService.requireAccessibleClass(classId);
        AnalyticsWindow window = parseWindow(range);
        AnalyticsBucket analyticsBucket = parseBucket(bucket);
        List<Long> studentIds = teachingClassService.listActiveStudentIds(classId);
        long studentCount = studentIds.size();
        if (studentIds.isEmpty()) {
            return new ClassCompletionRateVO(0d, 0L, 0L, emptyCompletionTrend(analyticsBucket, window), List.of());
        }

        List<AnalyticsDailyAggregateEntity> rows = analyticsDailyAggregateMapper.selectList(Wrappers.<AnalyticsDailyAggregateEntity>lambdaQuery()
                .in(AnalyticsDailyAggregateEntity::getOwnerUserId, studentIds)
                .eq(AnalyticsDailyAggregateEntity::getSourceType, AnalyticsConstants.SOURCE_TRAINING)
                .eq(AnalyticsDailyAggregateEntity::getAggregationLevel, AnalyticsConstants.AGGREGATION_LEVEL_SUMMARY)
                .ge(AnalyticsDailyAggregateEntity::getStatDate, window.startDate())
                .le(AnalyticsDailyAggregateEntity::getStatDate, window.endDate())
                .orderByAsc(AnalyticsDailyAggregateEntity::getStatDate)
                .orderByAsc(AnalyticsDailyAggregateEntity::getId));

        Set<Long> completedStudents = new LinkedHashSet<>();
        Map<LocalDate, LinkedHashSet<Long>> bucketStudents = initCompletionBuckets(window, analyticsBucket);
        Map<String, LinkedHashSet<Long>> modeStudents = new LinkedHashMap<>();
        for (AnalyticsDailyAggregateEntity row : rows) {
            completedStudents.add(row.getOwnerUserId());
            LocalDate bucketStart = analyticsBucket.bucketStart(row.getStatDate());
            bucketStudents.computeIfAbsent(bucketStart, ignored -> new LinkedHashSet<>()).add(row.getOwnerUserId());
            modeStudents.computeIfAbsent(row.getTrainingMode(), ignored -> new LinkedHashSet<>()).add(row.getOwnerUserId());
        }

        AnalyticsTrendVO trend = new AnalyticsTrendVO(
                analyticsBucket.apiValue(),
                bucketStudents.keySet().stream().map(LocalDate::toString).toList(),
                List.of(
                        new AnalyticsSeriesVO(
                                "completionRate",
                                "训练完成率",
                                bucketStudents.values().stream()
                                        .map(set -> ratio(set.size(), studentCount))
                                        .toList()
                        ),
                        new AnalyticsSeriesVO(
                                "completedStudentCount",
                                "完成学生数",
                                bucketStudents.values().stream()
                                        .map(set -> (double) set.size())
                                        .toList()
                        )
                )
        );

        List<ClassCompletionByModeVO> byMode = modeStudents.entrySet().stream()
                .filter(entry -> entry.getKey() != null && !AnalyticsConstants.DIMENSION_ALL.equalsIgnoreCase(entry.getKey()))
                .sorted((left, right) -> Integer.compare(right.getValue().size(), left.getValue().size()))
                .map(entry -> new ClassCompletionByModeVO(
                        entry.getKey(),
                        ratio(entry.getValue().size(), studentCount),
                        entry.getValue().size(),
                        studentCount
                ))
                .toList();

        return new ClassCompletionRateVO(
                ratio(completedStudents.size(), studentCount),
                studentCount,
                completedStudents.size(),
                trend,
                byMode
        );
    }

    public List<StudentProfileSummaryVO> listStudentProfiles(Long classId) {
        teachingClassService.requireAccessibleClass(classId);
        List<Long> studentIds = teachingClassService.listActiveStudentIds(classId);
        if (studentIds.isEmpty()) {
            return List.of();
        }

        Map<Long, UserEntity> userMap = userMapper.selectBatchIds(studentIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        Map<Long, StudentProfileEntity> profileMap = studentProfileMapper.selectList(Wrappers.<StudentProfileEntity>lambdaQuery()
                        .in(StudentProfileEntity::getUserId, studentIds))
                .stream()
                .collect(Collectors.toMap(StudentProfileEntity::getUserId, Function.identity()));
        Map<Long, LearningProfileSnapshotEntity> snapshotMap = loadStudentSnapshotsByIds(studentIds).stream()
                .collect(Collectors.toMap(LearningProfileSnapshotEntity::getStudentUserId, Function.identity()));

        return studentIds.stream()
                .map(studentId -> toStudentProfileSummary(studentId, userMap.get(studentId), profileMap.get(studentId), snapshotMap.get(studentId)))
                .sorted((left, right) -> {
                    int riskCompare = Double.compare(right.recentNegativeTransferRisk(), left.recentNegativeTransferRisk());
                    if (riskCompare != 0) {
                        return riskCompare;
                    }
                    int reviewCompare = Integer.compare(right.pendingReviewCount(), left.pendingReviewCount());
                    if (reviewCompare != 0) {
                        return reviewCompare;
                    }
                    return Double.compare(right.recentAccuracy(), left.recentAccuracy());
                })
                .toList();
    }

    public TeacherStudentDetailVO getStudentDetailForTeacher(Long classId, Long studentUserId) {
        teachingClassService.requireAccessibleClass(classId);
        teachingClassService.requireStudentInClass(classId, studentUserId);

        List<StudentProfileSummaryVO> summaries = listStudentProfiles(classId);
        int classRank = 0;
        for (int index = 0; index < summaries.size(); index++) {
            if (Objects.equals(summaries.get(index).studentUserId(), studentUserId)) {
                classRank = index + 1;
                break;
            }
        }
        double percentile = summaries.isEmpty() || classRank == 0
                ? 0d
                : round((summaries.size() - classRank + 1) / (double) summaries.size());
        String studentName = summaries.stream()
                .filter(summary -> Objects.equals(summary.studentUserId(), studentUserId))
                .map(StudentProfileSummaryVO::studentName)
                .findFirst()
                .orElseGet(() -> {
                    UserEntity user = userMapper.selectById(studentUserId);
                    return user == null ? null : user.getDisplayName();
                });

        return new TeacherStudentDetailVO(
                studentUserId,
                studentName,
                classRank,
                percentile,
                buildStudentDetail(studentUserId)
        );
    }

    private StudentAnalyticsDetailVO buildStudentDetail(Long studentUserId) {
        AnalyticsWindow sevenDayWindow = parseWindow("7d");
        AnalyticsWindow thirtyDayWindow = parseWindow("30d");
        return new StudentAnalyticsDetailVO(
                buildStudentOverview(studentUserId),
                buildTrend(loadStudentSlices(studentUserId, sevenDayWindow, AnalyticsConstants.AGGREGATION_LEVEL_SUMMARY), AnalyticsBucket.DAY, sevenDayWindow),
                buildTrend(loadStudentSlices(studentUserId, thirtyDayWindow, AnalyticsConstants.AGGREGATION_LEVEL_SUMMARY), AnalyticsBucket.DAY, thirtyDayWindow),
                buildHeatmap(loadStudentSlices(studentUserId, thirtyDayWindow, AnalyticsConstants.AGGREGATION_LEVEL_PAIR), null, null, thirtyDayWindow),
                buildScatter(loadStudentSlices(studentUserId, thirtyDayWindow, AnalyticsConstants.AGGREGATION_LEVEL_PAIR)),
                buildHighRiskPairs(loadStudentSlices(studentUserId, thirtyDayWindow, AnalyticsConstants.AGGREGATION_LEVEL_PAIR), 10),
                buildErrorDistribution(loadStudentSlices(studentUserId, thirtyDayWindow, AnalyticsConstants.AGGREGATION_LEVEL_PAIR))
        );
    }

    private StudentAnalyticsOverviewVO buildStudentOverview(Long studentUserId) {
        AnalyticsWindow window = parseWindow("30d");
        StudentProfileEntity studentProfile = loadStudentProfile(studentUserId);
        LearningProfileSnapshotEntity snapshotEntity = loadStudentSnapshot(studentUserId);
        StudentAnalyticsSnapshotPayload snapshotPayload = snapshotEntity == null
                ? emptyStudentPayload(studentUserId)
                : readStudentSnapshotPayload(snapshotEntity);
        List<AggregateSlice> summaryRows = loadStudentSlices(studentUserId, window, AnalyticsConstants.AGGREGATION_LEVEL_SUMMARY);
        List<AggregateSlice> pairRows = loadStudentSlices(studentUserId, window, AnalyticsConstants.AGGREGATION_LEVEL_PAIR);
        return new StudentAnalyticsOverviewVO(
                studentUserId,
                snapshotPayload.studentName(),
                snapshotPayload.gradeName(),
                snapshotPayload.frenchLevel(),
                snapshotPayload.primaryRiskLevel(),
                snapshotPayload.recommendedTrainingMode(),
                buildStudentCards(snapshotEntity, snapshotPayload),
                buildStudentRadar(summaryRows, snapshotEntity),
                buildStudentContextPerformance(pairRows),
                snapshotPayload,
                achievementService.getAchievementWall(studentUserId),
                buildStudentLearningGoal(studentProfile, summaryRows)
        );
    }

    private StudentLearningGoalVO buildStudentLearningGoal(
            StudentProfileEntity studentProfile,
            List<AggregateSlice> summaryRows
    ) {
        int todayTrainingCompleted = summaryRows.stream()
                .filter(row -> AnalyticsConstants.SOURCE_TRAINING.equalsIgnoreCase(row.sourceType()))
                .filter(row -> LocalDate.now().equals(row.statDate()))
                .mapToInt(AggregateSlice::attemptCount)
                .sum();
        LocalDate weeklyStartDate = LocalDate.now().minusDays(6);
        MetricTotals weeklyTotals = new MetricTotals();
        summaryRows.stream()
                .filter(row -> !row.statDate().isBefore(weeklyStartDate))
                .forEach(weeklyTotals::add);

        Integer dailyTrainingTarget = studentProfile == null ? null : studentProfile.getDailyTrainingTarget();
        Integer weeklyAccuracyTarget = studentProfile == null ? null : studentProfile.getWeeklyAccuracyTarget();
        double weeklyAccuracyCurrent = percentage(ratio(weeklyTotals.correctCount, weeklyTotals.attemptCount));
        return new StudentLearningGoalVO(
                dailyTrainingTarget,
                todayTrainingCompleted,
                dailyTrainingTarget == null ? 0 : Math.max(dailyTrainingTarget - todayTrainingCompleted, 0),
                weeklyAccuracyTarget,
                weeklyAccuracyCurrent,
                weeklyAccuracyTarget == null ? 0d : round(weeklyAccuracyCurrent - weeklyAccuracyTarget.doubleValue()),
                dailyTrainingTarget != null || weeklyAccuracyTarget != null,
                studentProfile == null ? null : studentProfile.getLearningGoalsUpdatedAt()
        );
    }

    private ClassAnalyticsOverviewVO buildClassOverview(TeachingClassEntity teachingClass, AnalyticsWindow window) {
        LearningProfileSnapshotEntity snapshotEntity = loadClassSnapshot(teachingClass.getId());
        ClassAnalyticsSnapshotPayload snapshotPayload = snapshotEntity == null
                ? emptyClassPayload(teachingClass)
                : readClassSnapshotPayload(snapshotEntity, teachingClass);
        List<AggregateSlice> summaryRows = loadClassSlices(teachingClass.getId(), window, AnalyticsConstants.AGGREGATION_LEVEL_SUMMARY);
        return new ClassAnalyticsOverviewVO(
                teachingClass.getId(),
                teachingClass.getClassCode(),
                teachingClass.getClassName(),
                snapshotPayload.studentCount(),
                snapshotPayload.activeStudentCount(),
                snapshotPayload.highRiskStudentCount(),
                snapshotPayload.primaryRiskLevel(),
                buildClassCards(summaryRows, snapshotPayload),
                buildClassRadar(summaryRows, snapshotPayload),
                snapshotPayload
        );
    }

    private List<AnalyticsCardVO> buildStudentCards(
            LearningProfileSnapshotEntity snapshotEntity,
            StudentAnalyticsSnapshotPayload snapshotPayload
    ) {
        return List.of(
                new AnalyticsCardVO("recentAccuracy", "近30天正确率", "%", percentage(snapshotEntity == null ? snapshotPayload.recentAccuracy() : decimal(snapshotEntity.getRecentAccuracy()))),
                new AnalyticsCardVO("negativeTransferRisk", "近30天负迁移风险", "%", percentage(snapshotEntity == null ? snapshotPayload.recentNegativeTransferRisk() : decimal(snapshotEntity.getRecentNegativeTransferRisk()))),
                new AnalyticsCardVO("avgReactionTimeMs", "近30天平均反应时", "ms", snapshotEntity == null ? snapshotPayload.recentAvgReactionTimeMs() : safeLong(snapshotEntity.getRecentAvgReactionTimeMs())),
                new AnalyticsCardVO("pendingReviewCount", "待复习词对数", "个", snapshotEntity == null ? snapshotPayload.pendingReviewCount() : safeInt(snapshotEntity.getPendingReviewCount()))
        );
    }

    private List<AnalyticsRadarMetricVO> buildStudentRadar(List<AggregateSlice> summaryRows, LearningProfileSnapshotEntity snapshotEntity) {
        MetricTotals totals = aggregateMetricTotals(summaryRows);
        return List.of(
                new AnalyticsRadarMetricVO("accuracy", "正确率", percentage(ratio(totals.correctCount, totals.attemptCount)), 100d),
                new AnalyticsRadarMetricVO("positiveTransfer", "正迁移", percentage(averageByCompletion(totals.positiveTransferScoreSum, totals.completionCount)), 100d),
                new AnalyticsRadarMetricVO("contextSensitivity", "语境敏感", percentage(averageByCompletion(totals.contextSensitivitySum, totals.completionCount)), 100d),
                new AnalyticsRadarMetricVO("semanticDiscrimination", "语义辨析", percentage(averageByCompletion(totals.semanticDiscriminationSum, totals.completionCount)), 100d),
                new AnalyticsRadarMetricVO("stability", "稳定度", percentage(1d - decimal(snapshotEntity == null ? null : snapshotEntity.getRecentNegativeTransferRisk())), 100d)
        );
    }

    private List<AnalyticsContextPerformanceVO> buildStudentContextPerformance(List<AggregateSlice> pairRows) {
        Map<String, MetricTotals> totals = new LinkedHashMap<>();
        for (String contextLevel : CONTEXT_ORDER) {
            totals.put(contextLevel, new MetricTotals());
        }
        for (AggregateSlice row : pairRows) {
            if (row.contextSupportLevel() == null || AnalyticsConstants.DIMENSION_ALL.equalsIgnoreCase(row.contextSupportLevel())) {
                continue;
            }
            totals.computeIfAbsent(row.contextSupportLevel(), ignored -> new MetricTotals()).add(row);
        }

        return CONTEXT_ORDER.stream()
                .map(contextLevel -> {
                    MetricTotals metricTotals = totals.getOrDefault(contextLevel, new MetricTotals());
                    return new AnalyticsContextPerformanceVO(
                            contextLabel(contextLevel),
                            ratio(metricTotals.correctCount, metricTotals.attemptCount),
                            averageReactionTime(metricTotals.totalReactionTimeMs, metricTotals.attemptCount),
                            metricTotals.attemptCount
                    );
                })
                .toList();
    }

    private List<AnalyticsCardVO> buildClassCards(List<AggregateSlice> summaryRows, ClassAnalyticsSnapshotPayload snapshotPayload) {
        MetricTotals totals = aggregateMetricTotals(summaryRows);
        return List.of(
                new AnalyticsCardVO("recentAccuracy", "区间正确率", "%", percentage(ratio(totals.correctCount, totals.attemptCount))),
                new AnalyticsCardVO("negativeTransferRisk", "区间负迁移风险", "%", percentage(averageByCompletion(totals.negativeTransferRiskSum, totals.completionCount))),
                new AnalyticsCardVO("avgReactionTimeMs", "区间平均反应时", "ms", averageReactionTime(totals.totalReactionTimeMs, totals.attemptCount)),
                new AnalyticsCardVO("highRiskStudentCount", "高风险学生数", "人", snapshotPayload.highRiskStudentCount())
        );
    }

    private List<AnalyticsRadarMetricVO> buildClassRadar(List<AggregateSlice> summaryRows, ClassAnalyticsSnapshotPayload snapshotPayload) {
        MetricTotals totals = aggregateMetricTotals(summaryRows);
        double activeRate = ratio(snapshotPayload.activeStudentCount(), snapshotPayload.studentCount());
        double highRiskRate = ratio(snapshotPayload.highRiskStudentCount(), snapshotPayload.studentCount());
        return List.of(
                new AnalyticsRadarMetricVO("accuracy", "正确率", percentage(ratio(totals.correctCount, totals.attemptCount)), 100d),
                new AnalyticsRadarMetricVO("positiveTransfer", "正迁移", percentage(averageByCompletion(totals.positiveTransferScoreSum, totals.completionCount)), 100d),
                new AnalyticsRadarMetricVO("contextSensitivity", "语境敏感", percentage(averageByCompletion(totals.contextSensitivitySum, totals.completionCount)), 100d),
                new AnalyticsRadarMetricVO("engagement", "活跃率", percentage(activeRate), 100d),
                new AnalyticsRadarMetricVO("riskPressure", "风险压力", percentage(highRiskRate), 100d)
        );
    }

    private AnalyticsTrendVO buildTrend(List<AggregateSlice> summaryRows, AnalyticsBucket bucket, AnalyticsWindow window) {
        Map<LocalDate, MetricTotals> bucketTotals = initMetricBuckets(window, bucket);
        for (AggregateSlice row : summaryRows) {
            bucketTotals.computeIfAbsent(bucket.bucketStart(row.statDate()), ignored -> new MetricTotals()).add(row);
        }

        List<String> xAxis = bucketTotals.keySet().stream().map(LocalDate::toString).toList();
        List<Double> positiveTransferValues = new ArrayList<>();
        List<Double> negativeRiskValues = new ArrayList<>();
        List<Double> accuracyValues = new ArrayList<>();
        List<Double> reactionValues = new ArrayList<>();
        for (MetricTotals totals : bucketTotals.values()) {
            positiveTransferValues.add(round(averageByCompletion(totals.positiveTransferScoreSum, totals.completionCount)));
            negativeRiskValues.add(round(averageByCompletion(totals.negativeTransferRiskSum, totals.completionCount)));
            accuracyValues.add(round(ratio(totals.correctCount, totals.attemptCount)));
            reactionValues.add((double) averageReactionTime(totals.totalReactionTimeMs, totals.attemptCount));
        }

        return new AnalyticsTrendVO(
                bucket.apiValue(),
                xAxis,
                List.of(
                        new AnalyticsSeriesVO("positiveTransferScore", "正迁移得分", positiveTransferValues),
                        new AnalyticsSeriesVO("negativeTransferRisk", "负迁移风险", negativeRiskValues),
                        new AnalyticsSeriesVO("accuracy", "正确率", accuracyValues),
                        new AnalyticsSeriesVO("avgReactionTimeMs", "平均反应时", reactionValues)
                )
        );
    }

    private AnalyticsHeatmapVO buildHeatmap(
            List<AggregateSlice> pairRows,
            String trainingModeFilter,
            String contextSupportLevelFilter,
            AnalyticsWindow window
    ) {
        Map<String, HeatmapCellAccumulator> cellMap = new LinkedHashMap<>();
        List<String> xAxis = PAIR_TYPE_ORDER.stream().map(this::pairTypeLabel).toList();
        List<String> yAxis = AnalyticsConstants.ERROR_TYPES.stream().map(this::errorLabel).toList();
        for (String xKey : xAxis) {
            for (String yKey : yAxis) {
                cellMap.put(xKey + "::" + yKey, new HeatmapCellAccumulator(xKey, yKey));
            }
        }

        for (AggregateSlice row : pairRows) {
            if (trainingModeFilter != null && !Objects.equals(trainingModeFilter, row.trainingMode())) {
                continue;
            }
            if (contextSupportLevelFilter != null && !Objects.equals(contextSupportLevelFilter, row.contextSupportLevel())) {
                continue;
            }
            String xKey = pairTypeLabel(row.lexicalPairType());
            if (!xAxis.contains(xKey)) {
                continue;
            }
            for (String errorType : AnalyticsConstants.ERROR_TYPES) {
                int errorCount = errorCount(row, errorType);
                if (errorCount <= 0) {
                    continue;
                }
                String yKey = errorLabel(errorType);
                cellMap.get(xKey + "::" + yKey).add(errorCount, row.correctCount(), row.attemptCount(), row.totalReactionTimeMs());
            }
        }

        Map<String, String> filters = new LinkedHashMap<>();
        filters.put("trainingMode", trainingModeFilter == null ? AnalyticsConstants.DIMENSION_ALL : modeLabel(trainingModeFilter));
        filters.put("contextSupportLevel", contextSupportLevelFilter == null ? AnalyticsConstants.DIMENSION_ALL : contextLabel(contextSupportLevelFilter));

        return new AnalyticsHeatmapVO(
                xAxis,
                yAxis,
                cellMap.values().stream().map(HeatmapCellAccumulator::toView).toList(),
                new AnalyticsHeatmapMetaVO(window.rawRange(), "pairType:errorType", filters)
        );
    }

    private AnalyticsScatterVO buildScatter(List<AggregateSlice> pairRows) {
        Map<Long, RiskPairAccumulator> accumulators = buildRiskPairAccumulators(pairRows);
        List<AnalyticsScatterPointVO> points = accumulators.values().stream()
                .map(RiskPairAccumulator::toScatterPoint)
                .sorted((left, right) -> {
                    int riskCompare = Double.compare(right.riskScore(), left.riskScore());
                    if (riskCompare != 0) {
                        return riskCompare;
                    }
                    return Integer.compare(right.attemptCount(), left.attemptCount());
                })
                .toList();
        return new AnalyticsScatterVO("avgReactionTimeMs", "accuracy", points);
    }

    private List<AnalyticsRiskPairVO> buildHighRiskPairs(List<AggregateSlice> pairRows, int limit) {
        return buildRiskPairAccumulators(pairRows).values().stream()
                .map(RiskPairAccumulator::toRiskPair)
                .sorted((left, right) -> {
                    int riskCompare = Double.compare(right.riskScore(), left.riskScore());
                    if (riskCompare != 0) {
                        return riskCompare;
                    }
                    return Integer.compare(right.incorrectCount(), left.incorrectCount());
                })
                .limit(limit)
                .toList();
    }

    private List<AnalyticsErrorDistributionVO> buildErrorDistribution(List<AggregateSlice> pairRows) {
        Map<String, Long> totals = initErrorTotals();
        for (AggregateSlice row : pairRows) {
            totals.compute("FALSE_FRIEND_CONFUSION", (key, value) -> value + row.falseFriendConfusionCount());
            totals.compute("CONTEXT_IGNORED", (key, value) -> value + row.contextIgnoredCount());
            totals.compute("OVER_TRANSFER", (key, value) -> value + row.overTransferCount());
            totals.compute("UNDER_TRANSFER", (key, value) -> value + row.underTransferCount());
            totals.compute("ORTHOGRAPHIC_INTERFERENCE", (key, value) -> value + row.orthographicInterferenceCount());
            totals.compute("SEMANTIC_MISFIRE", (key, value) -> value + row.semanticMisfireCount());
        }
        long total = totals.values().stream().mapToLong(Long::longValue).sum();
        return AnalyticsConstants.ERROR_TYPES.stream()
                .map(errorType -> new AnalyticsErrorDistributionVO(
                        errorType,
                        errorLabel(errorType),
                        totals.getOrDefault(errorType, 0L),
                        ratio(totals.getOrDefault(errorType, 0L), total)
                ))
                .toList();
    }

    private List<AnalyticsRiskBucketVO> buildRiskDistribution(List<Double> riskValues) {
        List<AnalyticsRiskBucketVO> buckets = new ArrayList<>();
        for (RiskBucketDefinition bucket : RISK_BUCKETS) {
            long studentCount = riskValues.stream()
                    .filter(value -> value >= bucket.bucketStart() && (value < bucket.bucketEnd() || bucket.bucketEnd() == 1.0d && value <= 1.0d))
                    .count();
            buckets.add(new AnalyticsRiskBucketVO(bucket.bucketStart(), bucket.bucketEnd(), studentCount));
        }
        return buckets;
    }

    private Map<Long, RiskPairAccumulator> buildRiskPairAccumulators(List<AggregateSlice> pairRows) {
        Map<Long, RiskPairAccumulator> accumulators = new LinkedHashMap<>();
        Map<Long, LexicalPairEntity> pairMap = loadLexicalPairMap(pairRows.stream()
                .map(AggregateSlice::lexicalPairId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList());
        for (AggregateSlice row : pairRows) {
            if (row.lexicalPairId() == null || row.lexicalPairId() <= 0) {
                continue;
            }
            LexicalPairEntity pair = pairMap.get(row.lexicalPairId());
            accumulators.computeIfAbsent(row.lexicalPairId(), ignored -> new RiskPairAccumulator(
                    row.lexicalPairId(),
                    pair == null ? null : pair.getEnglishWord(),
                    pair == null ? null : pair.getFrenchWord(),
                    pair == null ? row.lexicalPairType() : normalizePairType(pair.getLexicalPairType())
            )).add(row);
        }
        return accumulators;
    }

    private StudentProfileSummaryVO toStudentProfileSummary(
            Long studentUserId,
            UserEntity user,
            StudentProfileEntity studentProfile,
            LearningProfileSnapshotEntity snapshot
    ) {
        StudentAnalyticsSnapshotPayload payload = snapshot == null ? null : readStudentSnapshotPayload(snapshot);
        return new StudentProfileSummaryVO(
                studentUserId,
                payload != null && payload.studentName() != null ? payload.studentName() : user == null ? null : user.getDisplayName(),
                payload != null && payload.gradeName() != null ? payload.gradeName() : studentProfile == null ? null : studentProfile.getGradeName(),
                snapshot == null ? "LOW" : snapshot.getPrimaryRiskLevel(),
                snapshot == null ? 0d : decimal(snapshot.getRecentAccuracy()),
                snapshot == null ? 0d : decimal(snapshot.getRecentNegativeTransferRisk()),
                snapshot == null ? 0L : safeLong(snapshot.getRecentAvgReactionTimeMs()),
                snapshot == null ? 0 : safeInt(snapshot.getPendingReviewCount()),
                snapshot == null ? TrainingMode.COGNATE_BOOST.name() : snapshot.getRecommendedTrainingMode(),
                snapshot == null ? null : snapshot.getLastActiveAt()
        );
    }

    private LearningProfileSnapshotEntity loadStudentSnapshot(Long studentUserId) {
        return learningProfileSnapshotMapper.selectOne(Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                .eq(LearningProfileSnapshotEntity::getScope, AnalyticsConstants.PROFILE_SCOPE_STUDENT)
                .eq(LearningProfileSnapshotEntity::getStudentUserId, studentUserId)
                .last("LIMIT 1"));
    }

    private LearningProfileSnapshotEntity loadClassSnapshot(Long classId) {
        return learningProfileSnapshotMapper.selectOne(Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                .eq(LearningProfileSnapshotEntity::getScope, AnalyticsConstants.PROFILE_SCOPE_CLASS)
                .eq(LearningProfileSnapshotEntity::getTeachingClassId, classId)
                .last("LIMIT 1"));
    }

    private List<LearningProfileSnapshotEntity> loadStudentSnapshotsByIds(List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return List.of();
        }
        return learningProfileSnapshotMapper.selectList(Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                .eq(LearningProfileSnapshotEntity::getScope, AnalyticsConstants.PROFILE_SCOPE_STUDENT)
                .in(LearningProfileSnapshotEntity::getStudentUserId, studentIds));
    }

    private StudentAnalyticsSnapshotPayload readStudentSnapshotPayload(LearningProfileSnapshotEntity snapshotEntity) {
        StudentAnalyticsSnapshotPayload payload = analyticsJsonCodec.read(snapshotEntity.getSnapshotJson(), StudentAnalyticsSnapshotPayload.class);
        return payload == null ? emptyStudentPayload(snapshotEntity.getStudentUserId()) : payload;
    }

    private ClassAnalyticsSnapshotPayload readClassSnapshotPayload(
            LearningProfileSnapshotEntity snapshotEntity,
            TeachingClassEntity teachingClass
    ) {
        ClassAnalyticsSnapshotPayload payload = analyticsJsonCodec.read(snapshotEntity.getSnapshotJson(), ClassAnalyticsSnapshotPayload.class);
        return payload == null ? emptyClassPayload(teachingClass) : payload;
    }

    private StudentAnalyticsSnapshotPayload emptyStudentPayload(Long studentUserId) {
        UserEntity user = studentUserId == null ? null : userMapper.selectById(studentUserId);
        StudentProfileEntity studentProfile = loadStudentProfile(studentUserId);
        return new StudentAnalyticsSnapshotPayload(
                user == null ? null : user.getDisplayName(),
                studentProfile == null ? null : studentProfile.getGradeName(),
                studentProfile == null ? null : studentProfile.getFrenchLevel(),
                null,
                null,
                "LOW",
                TrainingMode.COGNATE_BOOST.name(),
                0,
                0,
                0d,
                0d,
                0L,
                null,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private StudentProfileEntity loadStudentProfile(Long studentUserId) {
        if (studentUserId == null) {
            return null;
        }
        return studentProfileMapper.selectOne(Wrappers.<StudentProfileEntity>lambdaQuery()
                .eq(StudentProfileEntity::getUserId, studentUserId)
                .last("LIMIT 1"));
    }

    private ClassAnalyticsSnapshotPayload emptyClassPayload(TeachingClassEntity teachingClass) {
        long studentCount = teachingClassService.countActiveStudents(teachingClass.getId());
        return new ClassAnalyticsSnapshotPayload(
                teachingClass.getClassCode(),
                teachingClass.getClassName(),
                teachingClass.getGradeName(),
                Math.toIntExact(studentCount),
                0,
                0,
                0d,
                0d,
                0L,
                "LOW",
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }

    private List<AggregateSlice> loadStudentSlices(Long studentUserId, AnalyticsWindow window, String aggregationLevel) {
        return analyticsDailyAggregateMapper.selectList(Wrappers.<AnalyticsDailyAggregateEntity>lambdaQuery()
                        .eq(AnalyticsDailyAggregateEntity::getOwnerUserId, studentUserId)
                        .eq(AnalyticsDailyAggregateEntity::getAggregationLevel, aggregationLevel)
                        .ge(AnalyticsDailyAggregateEntity::getStatDate, window.startDate())
                        .le(AnalyticsDailyAggregateEntity::getStatDate, window.endDate())
                        .orderByAsc(AnalyticsDailyAggregateEntity::getStatDate)
                        .orderByAsc(AnalyticsDailyAggregateEntity::getId))
                .stream()
                .map(AggregateSlice::fromStudentRow)
                .toList();
    }

    private List<AggregateSlice> loadClassSlices(Long classId, AnalyticsWindow window, String aggregationLevel) {
        return classAnalyticsDailyAggregateMapper.selectList(Wrappers.<ClassAnalyticsDailyAggregateEntity>lambdaQuery()
                        .eq(ClassAnalyticsDailyAggregateEntity::getTeachingClassId, classId)
                        .eq(ClassAnalyticsDailyAggregateEntity::getAggregationLevel, aggregationLevel)
                        .ge(ClassAnalyticsDailyAggregateEntity::getStatDate, window.startDate())
                        .le(ClassAnalyticsDailyAggregateEntity::getStatDate, window.endDate())
                        .orderByAsc(ClassAnalyticsDailyAggregateEntity::getStatDate)
                        .orderByAsc(ClassAnalyticsDailyAggregateEntity::getId))
                .stream()
                .map(AggregateSlice::fromClassRow)
                .toList();
    }

    private MetricTotals aggregateMetricTotals(List<AggregateSlice> rows) {
        MetricTotals totals = new MetricTotals();
        rows.forEach(totals::add);
        return totals;
    }

    private Map<String, Long> initErrorTotals() {
        Map<String, Long> totals = new LinkedHashMap<>();
        for (String errorType : AnalyticsConstants.ERROR_TYPES) {
            totals.put(errorType, 0L);
        }
        return totals;
    }

    private Map<LocalDate, MetricTotals> initMetricBuckets(AnalyticsWindow window, AnalyticsBucket bucket) {
        Map<LocalDate, MetricTotals> bucketTotals = new LinkedHashMap<>();
        if (bucket == AnalyticsBucket.DAY) {
            LocalDate cursor = window.startDate();
            while (!cursor.isAfter(window.endDate())) {
                bucketTotals.put(cursor, new MetricTotals());
                cursor = cursor.plusDays(1);
            }
            return bucketTotals;
        }

        LocalDate cursor = window.startDate().with(DayOfWeek.MONDAY);
        LocalDate end = window.endDate().with(DayOfWeek.MONDAY);
        while (!cursor.isAfter(end)) {
            bucketTotals.put(cursor, new MetricTotals());
            cursor = cursor.plusWeeks(1);
        }
        return bucketTotals;
    }

    private Map<LocalDate, LinkedHashSet<Long>> initCompletionBuckets(AnalyticsWindow window, AnalyticsBucket bucket) {
        Map<LocalDate, LinkedHashSet<Long>> bucketTotals = new LinkedHashMap<>();
        if (bucket == AnalyticsBucket.DAY) {
            LocalDate cursor = window.startDate();
            while (!cursor.isAfter(window.endDate())) {
                bucketTotals.put(cursor, new LinkedHashSet<>());
                cursor = cursor.plusDays(1);
            }
            return bucketTotals;
        }

        LocalDate cursor = window.startDate().with(DayOfWeek.MONDAY);
        LocalDate end = window.endDate().with(DayOfWeek.MONDAY);
        while (!cursor.isAfter(end)) {
            bucketTotals.put(cursor, new LinkedHashSet<>());
            cursor = cursor.plusWeeks(1);
        }
        return bucketTotals;
    }

    private AnalyticsTrendVO emptyCompletionTrend(AnalyticsBucket bucket, AnalyticsWindow window) {
        Map<LocalDate, LinkedHashSet<Long>> emptyBuckets = initCompletionBuckets(window, bucket);
        return new AnalyticsTrendVO(
                bucket.apiValue(),
                emptyBuckets.keySet().stream().map(LocalDate::toString).toList(),
                List.of(
                        new AnalyticsSeriesVO("completionRate", "训练完成率", emptyBuckets.values().stream().map(ignored -> 0d).toList()),
                        new AnalyticsSeriesVO("completedStudentCount", "完成学生数", emptyBuckets.values().stream().map(ignored -> 0d).toList())
                )
        );
    }

    private Map<Long, LexicalPairEntity> loadLexicalPairMap(Collection<Long> pairIds) {
        if (pairIds.isEmpty()) {
            return Map.of();
        }
        return lexicalPairMapper.selectBatchIds(new LinkedHashSet<>(pairIds)).stream()
                .collect(Collectors.toMap(LexicalPairEntity::getId, Function.identity()));
    }

    private AnalyticsWindow parseWindow(String range) {
        String normalized = range == null || range.isBlank() ? "30d" : range.trim().toLowerCase(Locale.ROOT);
        int days = switch (normalized) {
            case "7d" -> 7;
            case "30d" -> 30;
            default -> throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported range: " + range, 400);
        };
        LocalDate endDate = LocalDate.now();
        return new AnalyticsWindow(normalized, endDate.minusDays(days - 1L), endDate);
    }

    private AnalyticsBucket parseBucket(String bucket) {
        if (bucket == null || bucket.isBlank()) {
            return AnalyticsBucket.DAY;
        }
        return switch (bucket.trim().toLowerCase(Locale.ROOT)) {
            case "day" -> AnalyticsBucket.DAY;
            case "week" -> AnalyticsBucket.WEEK;
            default -> throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported bucket: " + bucket, 400);
        };
    }

    private String normalizeOptionalTrainingMode(String trainingMode) {
        if (trainingMode == null || trainingMode.isBlank()) {
            return null;
        }
        return TrainingMode.fromCode(trainingMode.trim()).name();
    }

    private String normalizeOptionalContextLevel(String contextSupportLevel) {
        if (contextSupportLevel == null || contextSupportLevel.isBlank()) {
            return null;
        }
        return ContextSupportLevel.fromCode(contextSupportLevel.trim()).name();
    }

    private String normalizePairType(String lexicalPairType) {
        if (lexicalPairType == null || lexicalPairType.isBlank() || AnalyticsConstants.DIMENSION_ALL.equalsIgnoreCase(lexicalPairType)) {
            return AnalyticsConstants.DIMENSION_ALL;
        }
        return LexicalPairType.fromCode(lexicalPairType).name();
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 10;
        }
        if (limit > 50) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Limit cannot exceed 50", 400);
        }
        return limit;
    }

    private Long requireCurrentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }

    private String errorLabel(String errorType) {
        return ERROR_LABELS.getOrDefault(errorType, errorType);
    }

    private String pairTypeLabel(String pairType) {
        return PAIR_TYPE_LABELS.getOrDefault(pairType, pairType == null ? AnalyticsConstants.DIMENSION_ALL : pairType);
    }

    private String modeLabel(String mode) {
        return MODE_LABELS.getOrDefault(mode, mode);
    }

    private String contextLabel(String contextLevel) {
        return CONTEXT_LABELS.getOrDefault(contextLevel, contextLevel);
    }

    private double ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0d;
        }
        return round(numerator / (double) denominator);
    }

    private double averageByCompletion(double sum, long completionCount) {
        if (completionCount <= 0) {
            return 0d;
        }
        return round(sum / completionCount);
    }

    private long averageReactionTime(long totalReactionTimeMs, long attemptCount) {
        if (attemptCount <= 0) {
            return 0L;
        }
        return Math.round(totalReactionTimeMs / (double) attemptCount);
    }

    private double percentage(double raw) {
        return round(raw * 100d);
    }

    private double decimal(BigDecimal value) {
        return value == null ? 0d : value.doubleValue();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private int errorCount(AggregateSlice row, String errorType) {
        return switch (errorType) {
            case "FALSE_FRIEND_CONFUSION" -> row.falseFriendConfusionCount();
            case "CONTEXT_IGNORED" -> row.contextIgnoredCount();
            case "OVER_TRANSFER" -> row.overTransferCount();
            case "UNDER_TRANSFER" -> row.underTransferCount();
            case "ORTHOGRAPHIC_INTERFERENCE" -> row.orthographicInterferenceCount();
            case "SEMANTIC_MISFIRE" -> row.semanticMisfireCount();
            default -> 0;
        };
    }

    private record AnalyticsWindow(
            String rawRange,
            LocalDate startDate,
            LocalDate endDate
    ) {
    }

    private enum AnalyticsBucket {
        DAY("day") {
            @Override
            LocalDate bucketStart(LocalDate date) {
                return date;
            }
        },
        WEEK("week") {
            @Override
            LocalDate bucketStart(LocalDate date) {
                return date.with(DayOfWeek.MONDAY);
            }
        };

        private final String apiValue;

        AnalyticsBucket(String apiValue) {
            this.apiValue = apiValue;
        }

        abstract LocalDate bucketStart(LocalDate date);

        String apiValue() {
            return apiValue;
        }
    }

    private record RiskBucketDefinition(
            double bucketStart,
            double bucketEnd
    ) {
    }

    private record AggregateSlice(
            LocalDate statDate,
            LocalDate weekStartDate,
            String sourceType,
            String aggregationLevel,
            Long lexicalPairId,
            String lexicalPairType,
            String trainingMode,
            String contextSupportLevel,
            int attemptCount,
            int correctCount,
            int incorrectCount,
            long totalReactionTimeMs,
            long totalHesitationTimeMs,
            double positiveTransferScoreSum,
            double negativeTransferRiskSum,
            double contextSensitivitySum,
            double semanticDiscriminationSum,
            int highRiskCount,
            int pendingReviewCount,
            int completionCount,
            int falseFriendConfusionCount,
            int contextIgnoredCount,
            int overTransferCount,
            int underTransferCount,
            int orthographicInterferenceCount,
            int semanticMisfireCount,
            LocalDateTime lastEventAt
    ) {

        static AggregateSlice fromStudentRow(AnalyticsDailyAggregateEntity row) {
            return new AggregateSlice(
                    row.getStatDate(),
                    row.getWeekStartDate(),
                    row.getSourceType(),
                    row.getAggregationLevel(),
                    row.getLexicalPairId(),
                    row.getLexicalPairType(),
                    row.getTrainingMode(),
                    row.getContextSupportLevel(),
                    row.getAttemptCount() == null ? 0 : row.getAttemptCount(),
                    row.getCorrectCount() == null ? 0 : row.getCorrectCount(),
                    row.getIncorrectCount() == null ? 0 : row.getIncorrectCount(),
                    row.getTotalReactionTimeMs() == null ? 0L : row.getTotalReactionTimeMs(),
                    row.getTotalHesitationTimeMs() == null ? 0L : row.getTotalHesitationTimeMs(),
                    row.getPositiveTransferScoreSum() == null ? 0d : row.getPositiveTransferScoreSum().doubleValue(),
                    row.getNegativeTransferRiskSum() == null ? 0d : row.getNegativeTransferRiskSum().doubleValue(),
                    row.getContextSensitivitySum() == null ? 0d : row.getContextSensitivitySum().doubleValue(),
                    row.getSemanticDiscriminationSum() == null ? 0d : row.getSemanticDiscriminationSum().doubleValue(),
                    row.getHighRiskCount() == null ? 0 : row.getHighRiskCount(),
                    row.getPendingReviewCount() == null ? 0 : row.getPendingReviewCount(),
                    row.getCompletionCount() == null ? 0 : row.getCompletionCount(),
                    row.getFalseFriendConfusionCount() == null ? 0 : row.getFalseFriendConfusionCount(),
                    row.getContextIgnoredCount() == null ? 0 : row.getContextIgnoredCount(),
                    row.getOverTransferCount() == null ? 0 : row.getOverTransferCount(),
                    row.getUnderTransferCount() == null ? 0 : row.getUnderTransferCount(),
                    row.getOrthographicInterferenceCount() == null ? 0 : row.getOrthographicInterferenceCount(),
                    row.getSemanticMisfireCount() == null ? 0 : row.getSemanticMisfireCount(),
                    row.getLastEventAt()
            );
        }

        static AggregateSlice fromClassRow(ClassAnalyticsDailyAggregateEntity row) {
            return new AggregateSlice(
                    row.getStatDate(),
                    row.getWeekStartDate(),
                    row.getSourceType(),
                    row.getAggregationLevel(),
                    row.getLexicalPairId(),
                    row.getLexicalPairType(),
                    row.getTrainingMode(),
                    row.getContextSupportLevel(),
                    row.getAttemptCount() == null ? 0 : row.getAttemptCount(),
                    row.getCorrectCount() == null ? 0 : row.getCorrectCount(),
                    row.getIncorrectCount() == null ? 0 : row.getIncorrectCount(),
                    row.getTotalReactionTimeMs() == null ? 0L : row.getTotalReactionTimeMs(),
                    row.getTotalHesitationTimeMs() == null ? 0L : row.getTotalHesitationTimeMs(),
                    row.getPositiveTransferScoreSum() == null ? 0d : row.getPositiveTransferScoreSum().doubleValue(),
                    row.getNegativeTransferRiskSum() == null ? 0d : row.getNegativeTransferRiskSum().doubleValue(),
                    row.getContextSensitivitySum() == null ? 0d : row.getContextSensitivitySum().doubleValue(),
                    row.getSemanticDiscriminationSum() == null ? 0d : row.getSemanticDiscriminationSum().doubleValue(),
                    row.getHighRiskCount() == null ? 0 : row.getHighRiskCount(),
                    row.getPendingReviewCount() == null ? 0 : row.getPendingReviewCount(),
                    row.getCompletionCount() == null ? 0 : row.getCompletionCount(),
                    row.getFalseFriendConfusionCount() == null ? 0 : row.getFalseFriendConfusionCount(),
                    row.getContextIgnoredCount() == null ? 0 : row.getContextIgnoredCount(),
                    row.getOverTransferCount() == null ? 0 : row.getOverTransferCount(),
                    row.getUnderTransferCount() == null ? 0 : row.getUnderTransferCount(),
                    row.getOrthographicInterferenceCount() == null ? 0 : row.getOrthographicInterferenceCount(),
                    row.getSemanticMisfireCount() == null ? 0 : row.getSemanticMisfireCount(),
                    row.getLastEventAt()
            );
        }
    }

    private static final class MetricTotals {

        private long attemptCount;
        private long correctCount;
        private long totalReactionTimeMs;
        private double positiveTransferScoreSum;
        private double negativeTransferRiskSum;
        private double contextSensitivitySum;
        private double semanticDiscriminationSum;
        private long completionCount;

        private void add(AggregateSlice row) {
            attemptCount += row.attemptCount();
            correctCount += row.correctCount();
            totalReactionTimeMs += row.totalReactionTimeMs();
            positiveTransferScoreSum += row.positiveTransferScoreSum();
            negativeTransferRiskSum += row.negativeTransferRiskSum();
            contextSensitivitySum += row.contextSensitivitySum();
            semanticDiscriminationSum += row.semanticDiscriminationSum();
            completionCount += row.completionCount();
        }
    }

    private static final class HeatmapCellAccumulator {

        private final String xKey;
        private final String yKey;
        private long value;
        private long correctCount;
        private long attemptCount;
        private long totalReactionTimeMs;

        private HeatmapCellAccumulator(String xKey, String yKey) {
            this.xKey = xKey;
            this.yKey = yKey;
        }

        private void add(long value, long correctCount, long attemptCount, long totalReactionTimeMs) {
            this.value += value;
            this.correctCount += correctCount;
            this.attemptCount += attemptCount;
            this.totalReactionTimeMs += totalReactionTimeMs;
        }

        private AnalyticsHeatmapCellVO toView() {
            double accuracy = attemptCount == 0 ? 0d : BigDecimal.valueOf(correctCount / (double) attemptCount)
                    .setScale(4, RoundingMode.HALF_UP)
                    .doubleValue();
            long averageReactionTime = attemptCount == 0 ? 0L : Math.round(totalReactionTimeMs / (double) attemptCount);
            return new AnalyticsHeatmapCellVO(xKey, yKey, value, accuracy, averageReactionTime);
        }
    }

    private static final class RiskPairAccumulator {

        private final Long lexicalPairId;
        private final String englishWord;
        private final String frenchWord;
        private final String lexicalPairType;
        private long attemptCount;
        private long correctCount;
        private long incorrectCount;
        private long totalReactionTimeMs;
        private double riskSum;
        private int highRiskCount;

        private RiskPairAccumulator(Long lexicalPairId, String englishWord, String frenchWord, String lexicalPairType) {
            this.lexicalPairId = lexicalPairId;
            this.englishWord = englishWord;
            this.frenchWord = frenchWord;
            this.lexicalPairType = lexicalPairType;
        }

        private void add(AggregateSlice row) {
            attemptCount += row.attemptCount();
            correctCount += row.correctCount();
            incorrectCount += row.incorrectCount();
            totalReactionTimeMs += row.totalReactionTimeMs();
            riskSum += row.negativeTransferRiskSum();
            highRiskCount += row.highRiskCount();
        }

        private double accuracy() {
            if (attemptCount == 0) {
                return 0d;
            }
            return BigDecimal.valueOf(correctCount / (double) attemptCount)
                    .setScale(4, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        private long averageReactionTimeMs() {
            if (attemptCount == 0) {
                return 0L;
            }
            return Math.round(totalReactionTimeMs / (double) attemptCount);
        }

        private double riskScore() {
            if (attemptCount == 0) {
                return 0d;
            }
            double averageRisk = riskSum / attemptCount;
            double incorrectRate = incorrectCount / (double) attemptCount;
            return BigDecimal.valueOf(Math.min(1d, Math.max(averageRisk, incorrectRate) + Math.min(0.2d, highRiskCount * 0.03d)))
                    .setScale(4, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        private AnalyticsRiskPairVO toRiskPair() {
            return new AnalyticsRiskPairVO(
                    lexicalPairId,
                    englishWord,
                    frenchWord,
                    lexicalPairType,
                    riskScore(),
                    Math.toIntExact(attemptCount),
                    Math.toIntExact(incorrectCount)
            );
        }

        private AnalyticsScatterPointVO toScatterPoint() {
            return new AnalyticsScatterPointVO(
                    lexicalPairId,
                    englishWord == null && frenchWord == null ? String.valueOf(lexicalPairId) : (englishWord + " / " + frenchWord),
                    lexicalPairType,
                    accuracy(),
                    averageReactionTimeMs(),
                    Math.toIntExact(attemptCount),
                    riskScore()
            );
        }
    }
}
