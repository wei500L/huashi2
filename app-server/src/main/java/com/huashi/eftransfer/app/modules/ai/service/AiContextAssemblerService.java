package com.huashi.eftransfer.app.modules.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.ai.support.AiConstants;
import com.huashi.eftransfer.app.modules.ai.support.AiDisplaySupport;
import com.huashi.eftransfer.app.modules.ai.vo.AiFocusLexicalPairVO;
import com.huashi.eftransfer.app.modules.analytics.entity.LearningProfileSnapshotEntity;
import com.huashi.eftransfer.app.modules.analytics.mapper.LearningProfileSnapshotMapper;
import com.huashi.eftransfer.app.modules.analytics.service.AnalyticsQueryService;
import com.huashi.eftransfer.app.modules.analytics.support.AnalyticsConstants;
import com.huashi.eftransfer.app.modules.analytics.support.AnalyticsJsonCodec;
import com.huashi.eftransfer.app.modules.analytics.support.StudentAnalyticsSnapshotPayload;
import com.huashi.eftransfer.app.modules.analytics.vo.StudentAnalyticsDetailVO;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisSummaryEntity;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisSummaryMapper;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisDistributionItem;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisHighRiskLexicalPair;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisJsonCodec;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairMapper;
import com.huashi.eftransfer.app.modules.training.entity.TrainingSessionEntity;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingSessionMapper;
import com.huashi.eftransfer.app.modules.training.support.TrainingJsonCodec;
import com.huashi.eftransfer.app.modules.training.support.TrainingSessionSummarySnapshot;
import com.huashi.eftransfer.app.modules.user.entity.StudentProfileEntity;
import com.huashi.eftransfer.app.modules.user.entity.UserEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.app.modules.user.mapper.UserMapper;
import com.huashi.eftransfer.shared.ai.RagDiagnosticSummary;
import com.huashi.eftransfer.shared.ai.RagErrorTypeStat;
import com.huashi.eftransfer.shared.ai.RagExplainRiskRequest;
import com.huashi.eftransfer.shared.ai.RagRiskLexicalPair;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AiContextAssemblerService {

    private final StudentProfileMapper studentProfileMapper;
    private final UserMapper userMapper;
    private final DiagnosisSummaryMapper diagnosisSummaryMapper;
    private final TrainingSessionMapper trainingSessionMapper;
    private final LearningProfileSnapshotMapper learningProfileSnapshotMapper;
    private final LexicalPairMapper lexicalPairMapper;
    private final DiagnosisJsonCodec diagnosisJsonCodec;
    private final TrainingJsonCodec trainingJsonCodec;
    private final AnalyticsJsonCodec analyticsJsonCodec;
    private final AnalyticsQueryService analyticsQueryService;

    public AiContextAssemblerService(
            StudentProfileMapper studentProfileMapper,
            UserMapper userMapper,
            DiagnosisSummaryMapper diagnosisSummaryMapper,
            TrainingSessionMapper trainingSessionMapper,
            LearningProfileSnapshotMapper learningProfileSnapshotMapper,
            LexicalPairMapper lexicalPairMapper,
            DiagnosisJsonCodec diagnosisJsonCodec,
            TrainingJsonCodec trainingJsonCodec,
            AnalyticsJsonCodec analyticsJsonCodec,
            AnalyticsQueryService analyticsQueryService
    ) {
        this.studentProfileMapper = studentProfileMapper;
        this.userMapper = userMapper;
        this.diagnosisSummaryMapper = diagnosisSummaryMapper;
        this.trainingSessionMapper = trainingSessionMapper;
        this.learningProfileSnapshotMapper = learningProfileSnapshotMapper;
        this.lexicalPairMapper = lexicalPairMapper;
        this.diagnosisJsonCodec = diagnosisJsonCodec;
        this.trainingJsonCodec = trainingJsonCodec;
        this.analyticsJsonCodec = analyticsJsonCodec;
        this.analyticsQueryService = analyticsQueryService;
    }

    public RecommendTrainingContext buildRecommendTrainingContext(Long studentUserId, Long diagnosisSummaryId) {
        StudentProfileEntity studentProfile = requireStudentProfile(studentUserId);
        UserEntity student = requireUser(studentUserId);
        DiagnosisSummaryEntity diagnosisSummary = requireDiagnosisSummary(studentUserId, diagnosisSummaryId);
        StudentAnalyticsSnapshotPayload snapshot = loadStudentSnapshot(studentUserId);
        List<DiagnosisHighRiskLexicalPair> summaryPairs = diagnosisJsonCodec.readHighRiskLexicalPairs(diagnosisSummary.getHighRiskLexicalPairsJson());
        List<AiFocusLexicalPairVO> focusPairs = buildFocusPairs(summaryPairs, snapshot);
        List<Map<String, Object>> recentTrainingRecords = loadRecentTrainingRecords(studentUserId);
        List<Map<String, Object>> commonErrors = buildCommonErrorTypes(diagnosisSummary, snapshot);

        Map<String, Object> promptPayload = new LinkedHashMap<>();
        promptPayload.put("studentProfile", buildStudentProfilePayload(studentProfile, student, snapshot));
        promptPayload.put("recentDiagnosisResult", buildDiagnosisSummaryPayload(diagnosisSummary, summaryPairs, commonErrors));
        promptPayload.put("recentTrainingRecords", recentTrainingRecords);
        promptPayload.put("highRiskLexicalPairs", focusPairs);
        promptPayload.put("commonErrorTypes", commonErrors);
        promptPayload.put("currentCourseStage", courseStage(studentProfile));
        promptPayload.put("focusTags", snapshot == null || snapshot.focusTags() == null ? List.of() : snapshot.focusTags());

        return new RecommendTrainingContext(
                studentUserId,
                diagnosisSummary.getId(),
                latestCompletedTrainingSessionId(studentUserId),
                courseStage(studentProfile),
                promptPayload,
                focusPairs,
                commonErrors,
                safeDouble(diagnosisSummary.getNegativeTransferRisk()),
                safeDouble(diagnosisSummary.getContextSensitivity()),
                safeDouble(diagnosisSummary.getSemanticDiscrimination()),
                safeDouble(diagnosisSummary.getOverallAccuracy()),
                safeLong(diagnosisSummary.getAverageReactionTimeMs()),
                snapshot == null ? null : snapshot.recommendedTrainingMode()
        );
    }

    public ExplainDiagnosisContext buildExplainDiagnosisContext(Long studentUserId, Long diagnosisSummaryId) {
        StudentProfileEntity studentProfile = requireStudentProfile(studentUserId);
        UserEntity student = requireUser(studentUserId);
        DiagnosisSummaryEntity diagnosisSummary = requireDiagnosisSummary(studentUserId, diagnosisSummaryId);
        StudentAnalyticsSnapshotPayload snapshot = loadStudentSnapshot(studentUserId);
        List<DiagnosisHighRiskLexicalPair> summaryPairs = diagnosisJsonCodec.readHighRiskLexicalPairs(diagnosisSummary.getHighRiskLexicalPairsJson());
        List<AiFocusLexicalPairVO> focusPairs = buildFocusPairs(summaryPairs, snapshot);
        List<Map<String, Object>> errorTypes = buildCommonErrorTypes(diagnosisSummary, snapshot);

        Map<String, Object> promptPayload = new LinkedHashMap<>();
        promptPayload.put("studentProfile", buildStudentProfilePayload(studentProfile, student, snapshot));
        promptPayload.put("diagnosisSummary", buildDiagnosisSummaryPayload(diagnosisSummary, summaryPairs, errorTypes));
        promptPayload.put("highRiskLexicalPairs", focusPairs);
        promptPayload.put("errorTypeDistribution", errorTypes);
        promptPayload.put("contextSensitivity", safeDouble(diagnosisSummary.getContextSensitivity()));
        promptPayload.put("semanticDiscrimination", safeDouble(diagnosisSummary.getSemanticDiscrimination()));
        promptPayload.put("currentCourseStage", courseStage(studentProfile));

        RagExplainRiskRequest ragExplainRiskRequest = new RagExplainRiskRequest(
                new RagDiagnosticSummary(
                        safeDouble(diagnosisSummary.getNegativeTransferRisk()),
                        safeDouble(diagnosisSummary.getContextSensitivity()),
                        safeDouble(diagnosisSummary.getOverallAccuracy()),
                        safeLong(diagnosisSummary.getAverageReactionTimeMs())
                ),
                errorTypes.stream()
                        .map(error -> new RagErrorTypeStat(
                                (String) error.get("code"),
                                (String) error.get("label"),
                                ((Number) error.get("count")).longValue(),
                                ((Number) error.get("ratio")).doubleValue()
                        ))
                        .toList(),
                focusPairs.stream()
                        .map(pair -> new RagRiskLexicalPair(
                                pair.lexicalPairId(),
                                pair.englishWord(),
                                pair.frenchWord(),
                                pair.chineseGloss(),
                                pair.lexicalPairType(),
                                pair.riskScore(),
                                1L,
                                safeLong(diagnosisSummary.getAverageReactionTimeMs()),
                                pair.dominantErrorType(),
                                AiDisplaySupport.riskLevelFromScore(pair.riskScore())
                        ))
                        .toList()
        );

        return new ExplainDiagnosisContext(
                studentUserId,
                diagnosisSummary.getId(),
                latestCompletedTrainingSessionId(studentUserId),
                courseStage(studentProfile),
                promptPayload,
                focusPairs,
                errorTypes,
                ragExplainRiskRequest,
                safeDouble(diagnosisSummary.getNegativeTransferRisk()),
                safeDouble(diagnosisSummary.getContextSensitivity()),
                safeDouble(diagnosisSummary.getSemanticDiscrimination()),
                safeDouble(diagnosisSummary.getOverallAccuracy()),
                safeLong(diagnosisSummary.getAverageReactionTimeMs())
        );
    }

    public TeacherInterventionContext buildTeacherInterventionContext(Long teacherUserId, Long classId, Long studentUserId, Long diagnosisSummaryId) {
        StudentProfileEntity studentProfile = requireStudentProfile(studentUserId);
        UserEntity student = requireUser(studentUserId);
        DiagnosisSummaryEntity diagnosisSummary = requireDiagnosisSummary(studentUserId, diagnosisSummaryId);
        StudentAnalyticsSnapshotPayload snapshot = loadStudentSnapshot(studentUserId);
        List<DiagnosisSummaryEntity> recentDiagnosisSummaries = diagnosisSummaryMapper.selectList(Wrappers.<DiagnosisSummaryEntity>lambdaQuery()
                        .eq(DiagnosisSummaryEntity::getOwnerUserId, studentUserId)
                        .orderByDesc(DiagnosisSummaryEntity::getGeneratedAt)
                        .orderByDesc(DiagnosisSummaryEntity::getId)
                        .last("LIMIT 3"));
        List<DiagnosisHighRiskLexicalPair> summaryPairs = diagnosisJsonCodec.readHighRiskLexicalPairs(diagnosisSummary.getHighRiskLexicalPairsJson());
        List<AiFocusLexicalPairVO> focusPairs = buildFocusPairs(summaryPairs, snapshot);
        List<Map<String, Object>> highRiskPatterns = new ArrayList<>(buildCommonErrorTypes(diagnosisSummary, snapshot));
        StudentAnalyticsDetailVO studentAnalyticsDetail = analyticsQueryService.getStudentDetail(studentUserId);

        Map<String, Object> promptPayload = new LinkedHashMap<>();
        promptPayload.put("studentProfile", buildStudentProfilePayload(studentProfile, student, snapshot));
        Map<String, Object> historicalTrends = new LinkedHashMap<>();
        historicalTrends.put("trend7d", studentAnalyticsDetail == null ? null : studentAnalyticsDetail.trend7d());
        historicalTrends.put("trend30d", studentAnalyticsDetail == null ? null : studentAnalyticsDetail.trend30d());
        promptPayload.put("historicalTrends", historicalTrends);
        promptPayload.put("recentThreeDiagnosesComparison", recentDiagnosisSummaries.stream()
                .map(this::buildDiagnosisComparisonPayload)
                .toList());
        promptPayload.put("trainingCompletion", buildTrainingCompletionPayload(studentUserId));
        Map<String, Object> highRiskPatternsPayload = new LinkedHashMap<>();
        highRiskPatternsPayload.put("focusLexicalPairs", focusPairs);
        highRiskPatternsPayload.put("errorTypeDistribution", highRiskPatterns);
        highRiskPatternsPayload.put("focusTags", snapshot == null || snapshot.focusTags() == null ? List.of() : snapshot.focusTags());
        promptPayload.put("highRiskPatterns", highRiskPatternsPayload);
        promptPayload.put("currentCourseStage", courseStage(studentProfile));

        return new TeacherInterventionContext(
                teacherUserId,
                classId,
                studentUserId,
                diagnosisSummary.getId(),
                latestCompletedTrainingSessionId(studentUserId),
                courseStage(studentProfile),
                promptPayload,
                focusPairs,
                highRiskPatterns,
                safeDouble(diagnosisSummary.getNegativeTransferRisk()),
                safeDouble(diagnosisSummary.getContextSensitivity()),
                safeDouble(diagnosisSummary.getSemanticDiscrimination()),
                safeDouble(diagnosisSummary.getOverallAccuracy()),
                safeLong(diagnosisSummary.getAverageReactionTimeMs()),
                student.getDisplayName()
        );
    }

    private Map<String, Object> buildStudentProfilePayload(
            StudentProfileEntity studentProfile,
            UserEntity student,
            StudentAnalyticsSnapshotPayload snapshot
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("studentUserId", studentProfile.getUserId());
        payload.put("studentName", student.getDisplayName());
        payload.put("studentNo", studentProfile.getStudentNo());
        payload.put("gradeName", studentProfile.getGradeName());
        payload.put("englishLevel", studentProfile.getEnglishLevel());
        payload.put("frenchLevel", studentProfile.getFrenchLevel());
        payload.put("courseStage", courseStage(studentProfile));
        payload.put("compositeScore", studentProfile.getCompositeScore());
        payload.put("primaryRiskLevel", snapshot == null ? "LOW" : snapshot.primaryRiskLevel());
        payload.put("recommendedTrainingMode", snapshot == null ? null : snapshot.recommendedTrainingMode());
        payload.put("pendingReviewCount", snapshot == null ? 0 : snapshot.pendingReviewCount());
        payload.put("recentAccuracy", snapshot == null ? 0d : snapshot.recentAccuracy());
        payload.put("recentNegativeTransferRisk", snapshot == null ? 0d : snapshot.recentNegativeTransferRisk());
        payload.put("recentAvgReactionTimeMs", snapshot == null ? 0L : snapshot.recentAvgReactionTimeMs());
        return payload;
    }

    private Map<String, Object> buildDiagnosisSummaryPayload(
            DiagnosisSummaryEntity diagnosisSummary,
            List<DiagnosisHighRiskLexicalPair> highRiskPairs,
            List<Map<String, Object>> errorTypes
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("diagnosisSummaryId", diagnosisSummary.getId());
        payload.put("sessionId", diagnosisSummary.getSessionId());
        payload.put("negativeTransferRisk", safeDouble(diagnosisSummary.getNegativeTransferRisk()));
        payload.put("contextSensitivity", safeDouble(diagnosisSummary.getContextSensitivity()));
        payload.put("semanticDiscrimination", safeDouble(diagnosisSummary.getSemanticDiscrimination()));
        payload.put("overallAccuracy", safeDouble(diagnosisSummary.getOverallAccuracy()));
        payload.put("averageReactionTimeMs", safeLong(diagnosisSummary.getAverageReactionTimeMs()));
        payload.put("generatedAt", diagnosisSummary.getGeneratedAt());
        payload.put("highRiskLexicalPairs", highRiskPairs);
        payload.put("errorTypeDistribution", errorTypes);
        return payload;
    }

    private List<AiFocusLexicalPairVO> buildFocusPairs(
            List<DiagnosisHighRiskLexicalPair> summaryPairs,
            StudentAnalyticsSnapshotPayload snapshot
    ) {
        Map<Long, AiFocusLexicalPairVO> focusPairs = new LinkedHashMap<>();
        Map<Long, LexicalPairEntity> pairMap = loadLexicalPairMap(summaryPairs.stream()
                .map(DiagnosisHighRiskLexicalPair::lexicalPairId)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        if (snapshot != null) {
            snapshot.topRiskPairs().forEach(pair -> {
                pairMap.putIfAbsent(pair.lexicalPairId(), lexicalPairMapper.selectById(pair.lexicalPairId()));
            });
        }

        for (DiagnosisHighRiskLexicalPair pair : summaryPairs) {
            LexicalPairEntity lexicalPair = pairMap.get(pair.lexicalPairId());
            focusPairs.put(pair.lexicalPairId(), new AiFocusLexicalPairVO(
                    pair.lexicalPairId(),
                    pair.englishWord(),
                    pair.frenchWord(),
                    lexicalPair == null ? pair.englishWord() + " / " + pair.frenchWord() : lexicalPair.getChineseGloss(),
                    pair.lexicalPairType(),
                    pair.riskScore(),
                    pair.dominantErrorType() == null ? "FALSE_FRIEND_CONFUSION" : pair.dominantErrorType(),
                    "最新诊断中该词对风险最高，需要优先纠偏。"
            ));
        }
        if (snapshot != null) {
            for (StudentAnalyticsSnapshotPayload.StudentRiskPairPayload pair : snapshot.topRiskPairs()) {
                LexicalPairEntity lexicalPair = pairMap.get(pair.lexicalPairId());
                focusPairs.putIfAbsent(pair.lexicalPairId(), new AiFocusLexicalPairVO(
                        pair.lexicalPairId(),
                        pair.englishWord(),
                        pair.frenchWord(),
                        lexicalPair == null ? pair.englishWord() + " / " + pair.frenchWord() : lexicalPair.getChineseGloss(),
                        pair.lexicalPairType(),
                        pair.riskScore(),
                        pair.riskScore() >= 0.7d ? "FALSE_FRIEND_CONFUSION" : "CONTEXT_IGNORED",
                        "近30天学情快照持续显示该词对为高风险模式。"
                ));
            }
        }
        List<AiFocusLexicalPairVO> result = focusPairs.values().stream()
                .sorted((left, right) -> Double.compare(right.riskScore(), left.riskScore()))
                .limit(5)
                .toList();
        if (!result.isEmpty()) {
            return result;
        }
        return List.of(new AiFocusLexicalPairVO(
                0L,
                "table",
                "table",
                "桌子",
                "COGNATE",
                0.10d,
                "UNDER_TRANSFER",
                "当前缺少稳定的高风险词对信号，先保留一组基础词对作为诊断与训练的启动参考。"
        ));
    }

    private List<Map<String, Object>> buildCommonErrorTypes(
            DiagnosisSummaryEntity diagnosisSummary,
            StudentAnalyticsSnapshotPayload snapshot
    ) {
        List<Map<String, Object>> errors = diagnosisJsonCodec.readDistributionItems(diagnosisSummary.getErrorTypeDistributionJson())
                .stream()
                .map(this::toErrorPayload)
                .sorted((left, right) -> Long.compare((Long) right.get("count"), (Long) left.get("count")))
                .toList();
        if (!errors.isEmpty()) {
            return errors;
        }
        if (snapshot == null) {
            return List.of();
        }
        return snapshot.errorDistribution().stream()
                .map(error -> Map.<String, Object>of(
                        "code", error.errorType(),
                        "label", AiDisplaySupport.errorLabel(error.errorType()),
                        "count", error.count(),
                        "ratio", error.ratio()
                ))
                .sorted((left, right) -> Long.compare((Long) right.get("count"), (Long) left.get("count")))
                .toList();
    }

    private Map<String, Object> toErrorPayload(DiagnosisDistributionItem item) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", item.code());
        payload.put("label", item.label() == null || item.label().isBlank() ? AiDisplaySupport.errorLabel(item.code()) : item.label());
        payload.put("count", item.count());
        payload.put("ratio", item.ratio());
        return payload;
    }

    private List<Map<String, Object>> loadRecentTrainingRecords(Long studentUserId) {
        return trainingSessionMapper.selectList(Wrappers.<TrainingSessionEntity>lambdaQuery()
                        .eq(TrainingSessionEntity::getOwnerUserId, studentUserId)
                        .isNotNull(TrainingSessionEntity::getCompletedAt)
                        .orderByDesc(TrainingSessionEntity::getCompletedAt)
                        .orderByDesc(TrainingSessionEntity::getId)
                        .last("LIMIT 3"))
                .stream()
                .map(this::toTrainingRecordPayload)
                .toList();
    }

    private Map<String, Object> buildTrainingCompletionPayload(Long studentUserId) {
        LocalDateTime rangeStart = LocalDateTime.now().minusDays(30);
        List<TrainingSessionEntity> recentSessions = trainingSessionMapper.selectList(Wrappers.<TrainingSessionEntity>lambdaQuery()
                .eq(TrainingSessionEntity::getOwnerUserId, studentUserId)
                .ge(TrainingSessionEntity::getStartedAt, rangeStart)
                .orderByDesc(TrainingSessionEntity::getStartedAt)
                .orderByDesc(TrainingSessionEntity::getId));
        long completedCount = recentSessions.stream().filter(session -> session.getCompletedAt() != null).count();
        double completionRate = recentSessions.isEmpty() ? 0d : completedCount / (double) recentSessions.size();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rangeDays", 30);
        payload.put("startedCount", recentSessions.size());
        payload.put("completedCount", completedCount);
        payload.put("completionRate", completionRate);
        payload.put("recentSessions", recentSessions.stream()
                .limit(3)
                .map(this::toTrainingRecordPayload)
                .toList());
        return payload;
    }

    private Map<String, Object> buildDiagnosisComparisonPayload(DiagnosisSummaryEntity summary) {
        List<DiagnosisHighRiskLexicalPair> highRiskPairs = diagnosisJsonCodec.readHighRiskLexicalPairs(summary.getHighRiskLexicalPairsJson());
        List<DiagnosisDistributionItem> errorDistribution = diagnosisJsonCodec.readDistributionItems(summary.getErrorTypeDistributionJson());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("diagnosisSummaryId", summary.getId());
        payload.put("generatedAt", summary.getGeneratedAt());
        payload.put("negativeTransferRisk", safeDouble(summary.getNegativeTransferRisk()));
        payload.put("contextSensitivity", safeDouble(summary.getContextSensitivity()));
        payload.put("semanticDiscrimination", safeDouble(summary.getSemanticDiscrimination()));
        payload.put("overallAccuracy", safeDouble(summary.getOverallAccuracy()));
        payload.put("averageReactionTimeMs", safeLong(summary.getAverageReactionTimeMs()));
        payload.put("topErrorType", errorDistribution.stream()
                .sorted((left, right) -> Long.compare(right.count(), left.count()))
                .map(DiagnosisDistributionItem::code)
                .findFirst()
                .orElse(null));
        payload.put("topRiskPairId", highRiskPairs.stream()
                .sorted((left, right) -> Double.compare(right.riskScore(), left.riskScore()))
                .map(DiagnosisHighRiskLexicalPair::lexicalPairId)
                .findFirst()
                .orElse(null));
        return payload;
    }

    private Map<String, Object> toTrainingRecordPayload(TrainingSessionEntity session) {
        TrainingSessionSummarySnapshot snapshot = session.getSummarySnapshotJson() == null || session.getSummarySnapshotJson().isBlank()
                ? null
                : trainingJsonCodec.readSummarySnapshot(session.getSummarySnapshotJson());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("trainingSessionId", session.getId());
        payload.put("mode", session.getMode());
        payload.put("modeLabel", session.getMode() == null ? null : AiDisplaySupport.modeLabel(session.getMode()));
        payload.put("status", session.getStatus());
        payload.put("completedAt", session.getCompletedAt());
        payload.put("accuracy", snapshot == null ? null : snapshot.accuracy());
        payload.put("averageReactionTimeMs", snapshot == null ? null : snapshot.averageReactionTime());
        payload.put("improvementHint", snapshot == null ? null : snapshot.improvementHint());
        payload.put("nextRecommendedMode", snapshot == null ? null : snapshot.nextRecommendedMode());
        return payload;
    }

    private StudentProfileEntity requireStudentProfile(Long studentUserId) {
        StudentProfileEntity studentProfile = studentProfileMapper.selectOne(Wrappers.<StudentProfileEntity>lambdaQuery()
                .eq(StudentProfileEntity::getUserId, studentUserId)
                .last("LIMIT 1"));
        if (studentProfile == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Student profile was not found", 404);
        }
        return studentProfile;
    }

    private UserEntity requireUser(Long studentUserId) {
        UserEntity user = userMapper.selectById(studentUserId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Student user was not found", 404);
        }
        return user;
    }

    private DiagnosisSummaryEntity requireDiagnosisSummary(Long studentUserId, Long diagnosisSummaryId) {
        DiagnosisSummaryEntity summary;
        if (diagnosisSummaryId == null) {
            summary = diagnosisSummaryMapper.selectOne(Wrappers.<DiagnosisSummaryEntity>lambdaQuery()
                    .eq(DiagnosisSummaryEntity::getOwnerUserId, studentUserId)
                    .orderByDesc(DiagnosisSummaryEntity::getGeneratedAt)
                    .orderByDesc(DiagnosisSummaryEntity::getId)
                    .last("LIMIT 1"));
        } else {
            summary = diagnosisSummaryMapper.selectById(diagnosisSummaryId);
            if (summary != null && !Objects.equals(summary.getOwnerUserId(), studentUserId)) {
                summary = null;
            }
        }
        if (summary == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Diagnosis summary was not found", 404);
        }
        return summary;
    }

    private StudentAnalyticsSnapshotPayload loadStudentSnapshot(Long studentUserId) {
        LearningProfileSnapshotEntity snapshot = learningProfileSnapshotMapper.selectOne(Wrappers.<LearningProfileSnapshotEntity>lambdaQuery()
                .eq(LearningProfileSnapshotEntity::getScope, AnalyticsConstants.PROFILE_SCOPE_STUDENT)
                .eq(LearningProfileSnapshotEntity::getStudentUserId, studentUserId)
                .last("LIMIT 1"));
        if (snapshot == null || snapshot.getSnapshotJson() == null || snapshot.getSnapshotJson().isBlank()) {
            return null;
        }
        return analyticsJsonCodec.read(snapshot.getSnapshotJson(), StudentAnalyticsSnapshotPayload.class);
    }

    private Map<Long, LexicalPairEntity> loadLexicalPairMap(Collection<Long> lexicalPairIds) {
        if (lexicalPairIds.isEmpty()) {
            return Map.of();
        }
        return lexicalPairMapper.selectBatchIds(new LinkedHashSet<>(lexicalPairIds))
                .stream()
                .collect(Collectors.toMap(LexicalPairEntity::getId, Function.identity()));
    }

    private String courseStage(StudentProfileEntity studentProfile) {
        if (studentProfile.getCourseStage() == null || studentProfile.getCourseStage().isBlank()) {
            return AiConstants.DEFAULT_COURSE_STAGE;
        }
        return studentProfile.getCourseStage().trim().toUpperCase(Locale.ROOT);
    }

    private Long latestCompletedTrainingSessionId(Long studentUserId) {
        TrainingSessionEntity session = trainingSessionMapper.selectOne(Wrappers.<TrainingSessionEntity>lambdaQuery()
                .eq(TrainingSessionEntity::getOwnerUserId, studentUserId)
                .isNotNull(TrainingSessionEntity::getCompletedAt)
                .orderByDesc(TrainingSessionEntity::getCompletedAt)
                .orderByDesc(TrainingSessionEntity::getId)
                .last("LIMIT 1"));
        return session == null ? null : session.getId();
    }

    private double safeDouble(Number value) {
        return value == null ? 0d : value.doubleValue();
    }

    private long safeLong(Number value) {
        return value == null ? 0L : value.longValue();
    }

    public record RecommendTrainingContext(
            Long studentUserId,
            Long diagnosisSummaryId,
            Long latestTrainingSessionId,
            String courseStage,
            Map<String, Object> promptPayload,
            List<AiFocusLexicalPairVO> focusPairs,
            List<Map<String, Object>> commonErrors,
            double negativeTransferRisk,
            double contextSensitivity,
            double semanticDiscrimination,
            double overallAccuracy,
            long averageReactionTimeMs,
            String recommendedTrainingMode
    ) {
    }

    public record ExplainDiagnosisContext(
            Long studentUserId,
            Long diagnosisSummaryId,
            Long latestTrainingSessionId,
            String courseStage,
            Map<String, Object> promptPayload,
            List<AiFocusLexicalPairVO> focusPairs,
            List<Map<String, Object>> errorTypes,
            RagExplainRiskRequest ragExplainRiskRequest,
            double negativeTransferRisk,
            double contextSensitivity,
            double semanticDiscrimination,
            double overallAccuracy,
            long averageReactionTimeMs
    ) {
    }

    public record TeacherInterventionContext(
            Long teacherUserId,
            Long classId,
            Long studentUserId,
            Long diagnosisSummaryId,
            Long latestTrainingSessionId,
            String courseStage,
            Map<String, Object> promptPayload,
            List<AiFocusLexicalPairVO> focusPairs,
            List<Map<String, Object>> highRiskPatterns,
            double negativeTransferRisk,
            double contextSensitivity,
            double semanticDiscrimination,
            double overallAccuracy,
            long averageReactionTimeMs,
            String studentName
    ) {
    }
}
