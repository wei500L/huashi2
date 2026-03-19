package com.huashi.eftransfer.app.modules.training.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.audit.service.AuditLogService;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisItemResultEntity;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisSessionEntity;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisSummaryEntity;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisTemplateItemEntity;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisItemResultMapper;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisSessionMapper;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisSummaryMapper;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisTemplateItemMapper;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisHighRiskLexicalPair;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisJsonCodec;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairExampleEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairSenseEntity;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairExampleMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairSenseMapper;
import com.huashi.eftransfer.app.modules.training.entity.ReviewScheduleEntity;
import com.huashi.eftransfer.app.modules.training.entity.TrainingPlanEntity;
import com.huashi.eftransfer.app.modules.training.entity.TrainingPlanItemEntity;
import com.huashi.eftransfer.app.modules.training.entity.WrongBookEntity;
import com.huashi.eftransfer.app.modules.training.mapper.ReviewScheduleMapper;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingPlanItemMapper;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingPlanMapper;
import com.huashi.eftransfer.app.modules.training.mapper.WrongBookMapper;
import com.huashi.eftransfer.app.modules.training.vo.RecommendedTrainingPairVO;
import com.huashi.eftransfer.app.modules.training.vo.RecommendedTrainingPlanVO;
import com.huashi.eftransfer.app.modules.training.vo.ReviewScheduleItemVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingSuggestedSessionVO;
import com.huashi.eftransfer.app.modules.training.vo.WrongBookItemVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.ContextSupportLevel;
import com.huashi.eftransfer.shared.enums.ReviewScheduleStatus;
import com.huashi.eftransfer.shared.enums.RiskLevel;
import com.huashi.eftransfer.shared.enums.TrainingPlanStatus;
import com.huashi.eftransfer.shared.enums.TrainingMode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TrainingPlanService {

    private static final Logger log = LoggerFactory.getLogger(TrainingPlanService.class);
    private static final int SLOW_REACTION_THRESHOLD_MS = 1200;

    private final DiagnosisSummaryMapper diagnosisSummaryMapper;
    private final DiagnosisSessionMapper diagnosisSessionMapper;
    private final DiagnosisItemResultMapper diagnosisItemResultMapper;
    private final DiagnosisTemplateItemMapper diagnosisTemplateItemMapper;
    private final LexicalPairMapper lexicalPairMapper;
    private final LexicalPairSenseMapper lexicalPairSenseMapper;
    private final LexicalPairExampleMapper lexicalPairExampleMapper;
    private final TrainingPlanMapper trainingPlanMapper;
    private final TrainingPlanItemMapper trainingPlanItemMapper;
    private final WrongBookMapper wrongBookMapper;
    private final ReviewScheduleMapper reviewScheduleMapper;
    private final DiagnosisJsonCodec diagnosisJsonCodec;
    private final com.huashi.eftransfer.app.modules.training.support.TrainingJsonCodec trainingJsonCodec;
    private final TrainingRecommendationEngine trainingRecommendationEngine;
    private final AuditLogService auditLogService;

    public TrainingPlanService(
            DiagnosisSummaryMapper diagnosisSummaryMapper,
            DiagnosisSessionMapper diagnosisSessionMapper,
            DiagnosisItemResultMapper diagnosisItemResultMapper,
            DiagnosisTemplateItemMapper diagnosisTemplateItemMapper,
            LexicalPairMapper lexicalPairMapper,
            LexicalPairSenseMapper lexicalPairSenseMapper,
            LexicalPairExampleMapper lexicalPairExampleMapper,
            TrainingPlanMapper trainingPlanMapper,
            TrainingPlanItemMapper trainingPlanItemMapper,
            WrongBookMapper wrongBookMapper,
            ReviewScheduleMapper reviewScheduleMapper,
            DiagnosisJsonCodec diagnosisJsonCodec,
            com.huashi.eftransfer.app.modules.training.support.TrainingJsonCodec trainingJsonCodec,
            TrainingRecommendationEngine trainingRecommendationEngine,
            AuditLogService auditLogService
    ) {
        this.diagnosisSummaryMapper = diagnosisSummaryMapper;
        this.diagnosisSessionMapper = diagnosisSessionMapper;
        this.diagnosisItemResultMapper = diagnosisItemResultMapper;
        this.diagnosisTemplateItemMapper = diagnosisTemplateItemMapper;
        this.lexicalPairMapper = lexicalPairMapper;
        this.lexicalPairSenseMapper = lexicalPairSenseMapper;
        this.lexicalPairExampleMapper = lexicalPairExampleMapper;
        this.trainingPlanMapper = trainingPlanMapper;
        this.trainingPlanItemMapper = trainingPlanItemMapper;
        this.wrongBookMapper = wrongBookMapper;
        this.reviewScheduleMapper = reviewScheduleMapper;
        this.diagnosisJsonCodec = diagnosisJsonCodec;
        this.trainingJsonCodec = trainingJsonCodec;
        this.trainingRecommendationEngine = trainingRecommendationEngine;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public RecommendedTrainingPlanVO getRecommendedPlan() {
        Long userId = currentUserId();
        LatestDiagnosisContext diagnosisContext = loadLatestDiagnosisContext(userId);
        TrainingPlanEntity plan = trainingPlanMapper.selectOne(Wrappers.<TrainingPlanEntity>lambdaQuery()
                .eq(TrainingPlanEntity::getOwnerUserId, userId)
                .eq(TrainingPlanEntity::getSourceDiagnosisSummaryId, diagnosisContext.summary().getId()));

        if (plan == null) {
            GeneratedPlan generatedPlan = generatePlan(userId, diagnosisContext);
            plan = generatedPlan.plan();
        }

        List<TrainingPlanItemEntity> planItems = loadPlanItems(plan.getId());
        return toRecommendedPlanVO(plan, planItems);
    }

    public List<WrongBookItemVO> getWrongBook() {
        Long userId = currentUserId();
        List<WrongBookEntity> wrongBooks = wrongBookMapper.selectList(Wrappers.<WrongBookEntity>lambdaQuery()
                .eq(WrongBookEntity::getOwnerUserId, userId)
                .orderByAsc(WrongBookEntity::getNextReviewAt)
                .orderByDesc(WrongBookEntity::getWrongCount)
                .orderByDesc(WrongBookEntity::getLastWrongAt));
        Map<Long, LexicalPairEntity> pairMap = loadLexicalPairMap(wrongBooks.stream().map(WrongBookEntity::getLexicalPairId).toList());
        return wrongBooks.stream()
                .map(wrongBook -> toWrongBookItemVO(wrongBook, pairMap.get(wrongBook.getLexicalPairId())))
                .toList();
    }

    public List<ReviewScheduleItemVO> getReviewSchedule(boolean pendingOnly) {
        Long userId = currentUserId();
        var wrapper = Wrappers.<ReviewScheduleEntity>lambdaQuery()
                .eq(ReviewScheduleEntity::getOwnerUserId, userId)
                .orderByAsc(ReviewScheduleEntity::getDueAt)
                .orderByAsc(ReviewScheduleEntity::getId);
        if (pendingOnly) {
            wrapper.eq(ReviewScheduleEntity::getStatus, ReviewScheduleStatus.PENDING.name());
        }
        List<ReviewScheduleEntity> schedules = reviewScheduleMapper.selectList(wrapper);
        Map<Long, LexicalPairEntity> pairMap = loadLexicalPairMap(schedules.stream().map(ReviewScheduleEntity::getLexicalPairId).toList());
        return schedules.stream()
                .map(schedule -> toReviewScheduleItemVO(schedule, pairMap.get(schedule.getLexicalPairId())))
                .toList();
    }

    private GeneratedPlan generatePlan(Long userId, LatestDiagnosisContext diagnosisContext) {
        List<WrongBookEntity> existingWrongBooks = wrongBookMapper.selectList(Wrappers.<WrongBookEntity>lambdaQuery()
                .eq(WrongBookEntity::getOwnerUserId, userId));
        Map<Long, Integer> repeatWrongMap = existingWrongBooks.stream()
                .collect(Collectors.toMap(WrongBookEntity::getLexicalPairId, WrongBookEntity::getWrongCount, Integer::max));

        TrainingRecommendationEngine.RecommendationContext recommendationContext = buildRecommendationContext(
                userId,
                diagnosisContext,
                repeatWrongMap
        );
        TrainingRecommendationEngine.TrainingRecommendation recommendation = trainingRecommendationEngine.recommend(recommendationContext);

        TrainingPlanEntity plan = new TrainingPlanEntity();
        plan.setOwnerUserId(userId);
        plan.setSourceDiagnosisSessionId(diagnosisContext.session().getId());
        plan.setSourceDiagnosisSummaryId(diagnosisContext.summary().getId());
        plan.setStatus(TrainingPlanStatus.GENERATED.name());
        plan.setPriorityMode(recommendation.priorityMode().name());
        plan.setRecommendedDifficulty(recommendation.recommendedDifficulty());
        plan.setRiskLevel(recommendation.riskLevel().name());
        plan.setEstimatedTrainingVolume(recommendation.estimatedTrainingVolume());
        plan.setRecommendationReason(recommendation.recommendationReason());
        plan.setTargetMetricsJson(trainingJsonCodec.write(recommendation.targetMetrics()));
        plan.setGeneratedAt(LocalDateTime.now());
        trainingPlanMapper.insert(plan);

        int order = 1;
        List<TrainingPlanItemEntity> planItems = recommendation.pairRecommendations().stream()
                .map(pairRecommendation -> {
                    TrainingPlanItemEntity entity = new TrainingPlanItemEntity();
                    entity.setPlanId(plan.getId());
                    entity.setLexicalPairId(pairRecommendation.lexicalPairId());
                    entity.setRecommendedMode(pairRecommendation.recommendedMode().name());
                    entity.setRecommendedDifficulty(pairRecommendation.recommendedDifficulty());
                    entity.setRiskLevel(pairRecommendation.riskLevel().name());
                    entity.setPriorityScore(decimal(pairRecommendation.priorityScore()));
                    entity.setRecommendedReason(pairRecommendation.recommendedReason());
                    entity.setDominantErrorType(pairRecommendation.dominantErrorType());
                    entity.setTargetContextSupport(pairRecommendation.targetContextSupport());
                    entity.setExpectedExposures(pairRecommendation.expectedExposures());
                    entity.setSortOrder(order++);
                    return entity;
                })
                .toList();
        for (TrainingPlanItemEntity planItem : planItems) {
            trainingPlanItemMapper.insert(planItem);
        }

        auditLogService.record("generate_training_plan", "training_plan", String.valueOf(plan.getId()),
                Map.of("diagnosisSummaryId", diagnosisContext.summary().getId(), "priorityMode", recommendation.priorityMode().name()),
                ResultCode.SUCCESS.code());
        log.info("event=training_plan_generated planId={} sourceDiagnosisSummaryId={} ownerUserId={} priorityMode={} estimatedTrainingVolume={}",
                plan.getId(), diagnosisContext.summary().getId(), userId, recommendation.priorityMode().name(), recommendation.estimatedTrainingVolume());
        return new GeneratedPlan(plan, planItems);
    }

    private TrainingRecommendationEngine.RecommendationContext buildRecommendationContext(
            Long userId,
            LatestDiagnosisContext diagnosisContext,
            Map<Long, Integer> repeatWrongMap
    ) {
        List<TrainingRecommendationEngine.PairSignal> pairSignals = diagnosisContext.itemResults().stream()
                .collect(Collectors.groupingBy(DiagnosisItemResultEntity::getLexicalPairId, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> toPairSignal(entry.getKey(), entry.getValue(), diagnosisContext, repeatWrongMap.getOrDefault(entry.getKey(), 0)))
                .filter(Objects::nonNull)
                .toList();

        return new TrainingRecommendationEngine.RecommendationContext(
                userId,
                diagnosisContext.session().getId(),
                diagnosisContext.summary().getId(),
                diagnosisContext.summary().getNegativeTransferRisk().doubleValue(),
                diagnosisContext.summary().getContextSensitivity().doubleValue(),
                diagnosisContext.summary().getOverallAccuracy().doubleValue(),
                diagnosisContext.summary().getAverageReactionTimeMs(),
                pairSignals
        );
    }

    private TrainingRecommendationEngine.PairSignal toPairSignal(
            Long lexicalPairId,
            List<DiagnosisItemResultEntity> itemResults,
            LatestDiagnosisContext diagnosisContext,
            int repeatWrongCount
    ) {
        LexicalPairEntity pair = diagnosisContext.lexicalPairMap().get(lexicalPairId);
        if (pair == null) {
            return null;
        }
        long errorCount = itemResults.stream().filter(result -> Boolean.FALSE.equals(result.getIsCorrect())).count();
        long correctCount = itemResults.stream().filter(result -> Boolean.TRUE.equals(result.getIsCorrect())).count();
        long slowCorrectCount = itemResults.stream()
                .filter(result -> Boolean.TRUE.equals(result.getIsCorrect()))
                .filter(result -> result.getReactionTimeMs() != null && result.getReactionTimeMs() >= SLOW_REACTION_THRESHOLD_MS)
                .count();
        long averageReactionTime = Math.round(itemResults.stream()
                .filter(result -> result.getReactionTimeMs() != null)
                .mapToInt(DiagnosisItemResultEntity::getReactionTimeMs)
                .average()
                .orElse(0));
        String dominantErrorType = dominantErrorType(itemResults);
        DiagnosisHighRiskLexicalPair highRiskPair = diagnosisContext.highRiskPairMap().get(lexicalPairId);
        double diagnosisRiskScore = highRiskPair == null
                ? itemResults.stream()
                .map(DiagnosisItemResultEntity::getTransferRiskScore)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .average()
                .orElse(0.0)
                : highRiskPair.riskScore();

        return new TrainingRecommendationEngine.PairSignal(
                lexicalPairId,
                pair.getEnglishWord(),
                pair.getFrenchWord(),
                pair.getChineseGloss(),
                pair.getLexicalPairType(),
                pair.getSemanticOverlapScore().doubleValue(),
                pair.getFalseFriendRisk().doubleValue(),
                pair.getDefaultContextSupport(),
                pair.getDifficultyLevel(),
                diagnosisRiskScore,
                errorCount,
                correctCount,
                slowCorrectCount,
                itemResults.size(),
                repeatWrongCount,
                averageReactionTime,
                dominantErrorType,
                countErrorType(itemResults, "FALSE_FRIEND_CONFUSION"),
                countErrorType(itemResults, "ORTHOGRAPHIC_INTERFERENCE"),
                countErrorType(itemResults, "CONTEXT_IGNORED"),
                countErrorType(itemResults, "UNDER_TRANSFER"),
                diagnosisContext.hasContextExampleMap().getOrDefault(lexicalPairId, false)
        );
    }

    private LatestDiagnosisContext loadLatestDiagnosisContext(Long userId) {
        DiagnosisSummaryEntity summary = diagnosisSummaryMapper.selectOne(Wrappers.<DiagnosisSummaryEntity>lambdaQuery()
                .eq(DiagnosisSummaryEntity::getOwnerUserId, userId)
                .orderByDesc(DiagnosisSummaryEntity::getGeneratedAt)
                .orderByDesc(DiagnosisSummaryEntity::getId)
                .last("LIMIT 1"));
        if (summary == null) {
            throw new BusinessException(ResultCode.CONFLICT, "Please complete a diagnosis before requesting training recommendations", 409);
        }
        DiagnosisSessionEntity session = diagnosisSessionMapper.selectById(summary.getSessionId());
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Latest diagnosis session was not found", 404);
        }
        List<DiagnosisItemResultEntity> itemResults = diagnosisItemResultMapper.selectList(Wrappers.<DiagnosisItemResultEntity>lambdaQuery()
                .eq(DiagnosisItemResultEntity::getSessionId, session.getId())
                .orderByAsc(DiagnosisItemResultEntity::getPresentationOrder)
                .orderByAsc(DiagnosisItemResultEntity::getId));
        if (itemResults.isEmpty()) {
            throw new BusinessException(ResultCode.CONFLICT, "Latest diagnosis result does not contain item-level signals", 409);
        }

        Map<Long, DiagnosisTemplateItemEntity> templateItemMap = diagnosisTemplateItemMapper.selectBatchIds(itemResults.stream()
                        .map(DiagnosisItemResultEntity::getTemplateItemId)
                        .collect(Collectors.toCollection(LinkedHashSet::new)))
                .stream()
                .collect(Collectors.toMap(DiagnosisTemplateItemEntity::getId, Function.identity()));
        Map<Long, LexicalPairEntity> lexicalPairMap = loadLexicalPairMap(itemResults.stream().map(DiagnosisItemResultEntity::getLexicalPairId).toList());
        Map<Long, DiagnosisHighRiskLexicalPair> highRiskPairMap = diagnosisJsonCodec.readHighRiskLexicalPairs(summary.getHighRiskLexicalPairsJson())
                .stream()
                .collect(Collectors.toMap(DiagnosisHighRiskLexicalPair::lexicalPairId, Function.identity(), (left, right) -> left));
        Map<Long, Boolean> hasContextExampleMap = loadContextExampleAvailability(lexicalPairMap.keySet());

        return new LatestDiagnosisContext(summary, session, itemResults, templateItemMap, lexicalPairMap, highRiskPairMap, hasContextExampleMap);
    }

    private Map<Long, Boolean> loadContextExampleAvailability(Collection<Long> lexicalPairIds) {
        if (lexicalPairIds.isEmpty()) {
            return Map.of();
        }
        List<LexicalPairSenseEntity> senses = lexicalPairSenseMapper.selectList(Wrappers.<LexicalPairSenseEntity>lambdaQuery()
                .in(LexicalPairSenseEntity::getLexicalPairId, lexicalPairIds));
        if (senses.isEmpty()) {
            return lexicalPairIds.stream().collect(Collectors.toMap(Function.identity(), ignored -> false));
        }
        Map<Long, Long> senseToPair = senses.stream()
                .collect(Collectors.toMap(LexicalPairSenseEntity::getId, LexicalPairSenseEntity::getLexicalPairId));
        Map<Long, Boolean> result = lexicalPairIds.stream()
                .collect(Collectors.toMap(Function.identity(), ignored -> false));
        List<LexicalPairExampleEntity> examples = lexicalPairExampleMapper.selectList(Wrappers.<LexicalPairExampleEntity>lambdaQuery()
                .in(LexicalPairExampleEntity::getLexicalPairSenseId, senseToPair.keySet()));
        for (LexicalPairExampleEntity example : examples) {
            if (example.getContextSupportLevel() == null) {
                continue;
            }
            ContextSupportLevel contextSupportLevel = ContextSupportLevel.fromCode(example.getContextSupportLevel());
            if (contextSupportLevel == ContextSupportLevel.MEDIUM || contextSupportLevel == ContextSupportLevel.HIGH) {
                Long pairId = senseToPair.get(example.getLexicalPairSenseId());
                if (pairId != null) {
                    result.put(pairId, true);
                }
            }
        }
        return result;
    }

    private List<TrainingPlanItemEntity> loadPlanItems(Long planId) {
        return trainingPlanItemMapper.selectList(Wrappers.<TrainingPlanItemEntity>lambdaQuery()
                .eq(TrainingPlanItemEntity::getPlanId, planId)
                .orderByDesc(TrainingPlanItemEntity::getPriorityScore)
                .orderByAsc(TrainingPlanItemEntity::getSortOrder)
                .orderByAsc(TrainingPlanItemEntity::getId));
    }

    private RecommendedTrainingPlanVO toRecommendedPlanVO(TrainingPlanEntity plan, List<TrainingPlanItemEntity> planItems) {
        Map<Long, LexicalPairEntity> pairMap = loadLexicalPairMap(planItems.stream().map(TrainingPlanItemEntity::getLexicalPairId).toList());
        List<RecommendedTrainingPairVO> recommendedPairs = planItems.stream()
                .map(planItem -> toRecommendedTrainingPairVO(planItem, pairMap.get(planItem.getLexicalPairId())))
                .toList();
        List<TrainingSuggestedSessionVO> suggestedSessions = planItems.stream()
                .collect(Collectors.groupingBy(TrainingPlanItemEntity::getRecommendedMode, LinkedHashMap::new, Collectors.summingInt(TrainingPlanItemEntity::getExpectedExposures)))
                .entrySet()
                .stream()
                .map(entry -> new TrainingSuggestedSessionVO(entry.getKey(), modeLabel(entry.getKey()), entry.getValue()))
                .sorted(Comparator.comparingInt(TrainingSuggestedSessionVO::count).reversed()
                        .thenComparing(TrainingSuggestedSessionVO::mode))
                .toList();
        return new RecommendedTrainingPlanVO(
                plan.getId(),
                plan.getSourceDiagnosisSessionId(),
                plan.getSourceDiagnosisSummaryId(),
                plan.getStatus(),
                plan.getPriorityMode(),
                plan.getRecommendedDifficulty(),
                plan.getRiskLevel(),
                plan.getEstimatedTrainingVolume(),
                plan.getRecommendationReason(),
                trainingJsonCodec.readStringList(plan.getTargetMetricsJson()),
                suggestedSessions,
                recommendedPairs,
                plan.getGeneratedAt()
        );
    }

    private RecommendedTrainingPairVO toRecommendedTrainingPairVO(TrainingPlanItemEntity planItem, LexicalPairEntity pair) {
        return new RecommendedTrainingPairVO(
                planItem.getId(),
                planItem.getLexicalPairId(),
                pair == null ? null : pair.getEnglishWord(),
                pair == null ? null : pair.getFrenchWord(),
                pair == null ? null : pair.getChineseGloss(),
                pair == null ? null : pair.getLexicalPairType(),
                planItem.getRecommendedMode(),
                planItem.getRecommendedDifficulty(),
                planItem.getRiskLevel(),
                planItem.getPriorityScore().doubleValue(),
                planItem.getRecommendedReason(),
                planItem.getDominantErrorType(),
                planItem.getExpectedExposures(),
                planItem.getTargetContextSupport()
        );
    }

    private WrongBookItemVO toWrongBookItemVO(WrongBookEntity wrongBook, LexicalPairEntity pair) {
        return new WrongBookItemVO(
                wrongBook.getId(),
                wrongBook.getLexicalPairId(),
                pair == null ? null : pair.getEnglishWord(),
                pair == null ? null : pair.getFrenchWord(),
                pair == null ? null : pair.getChineseGloss(),
                pair == null ? null : pair.getLexicalPairType(),
                wrongBook.getWrongCount(),
                wrongBook.getLastErrorType(),
                wrongBook.getMasteryStatus(),
                wrongBook.getFirstWrongAt(),
                wrongBook.getLastWrongAt(),
                wrongBook.getNextReviewAt()
        );
    }

    private ReviewScheduleItemVO toReviewScheduleItemVO(ReviewScheduleEntity schedule, LexicalPairEntity pair) {
        return new ReviewScheduleItemVO(
                schedule.getId(),
                schedule.getWrongBookId(),
                schedule.getLexicalPairId(),
                pair == null ? null : pair.getEnglishWord(),
                pair == null ? null : pair.getFrenchWord(),
                pair == null ? null : pair.getChineseGloss(),
                pair == null ? null : pair.getLexicalPairType(),
                schedule.getScheduleStage(),
                schedule.getIntervalDays(),
                schedule.getDueAt(),
                schedule.getStatus(),
                schedule.getReviewMode(),
                schedule.getTriggerReason()
        );
    }

    private Map<Long, LexicalPairEntity> loadLexicalPairMap(Collection<Long> lexicalPairIds) {
        if (lexicalPairIds.isEmpty()) {
            return Map.of();
        }
        return lexicalPairMapper.selectBatchIds(new LinkedHashSet<>(lexicalPairIds))
                .stream()
                .collect(Collectors.toMap(LexicalPairEntity::getId, Function.identity()));
    }

    private long countErrorType(List<DiagnosisItemResultEntity> itemResults, String errorType) {
        return itemResults.stream()
                .map(DiagnosisItemResultEntity::getDetectedErrorType)
                .filter(Objects::nonNull)
                .filter(value -> value.equalsIgnoreCase(errorType))
                .count();
    }

    private String dominantErrorType(List<DiagnosisItemResultEntity> itemResults) {
        return itemResults.stream()
                .map(DiagnosisItemResultEntity::getDetectedErrorType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.<String, Long>comparingByValue()
                        .thenComparing(Map.Entry::getKey))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private String modeLabel(String mode) {
        return switch (TrainingMode.fromCode(mode)) {
            case FALSE_FRIEND_DISCRIM -> "纠偏：同形异义词辨析";
            case CONTEXT_FIX -> "修复：语境纠偏";
            case SPEED_CHALLENGE -> "提速：快速识别";
            case COGNATE_BOOST -> "强化：正迁移促进";
        };
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }

    private record LatestDiagnosisContext(
            DiagnosisSummaryEntity summary,
            DiagnosisSessionEntity session,
            List<DiagnosisItemResultEntity> itemResults,
            Map<Long, DiagnosisTemplateItemEntity> templateItemMap,
            Map<Long, LexicalPairEntity> lexicalPairMap,
            Map<Long, DiagnosisHighRiskLexicalPair> highRiskPairMap,
            Map<Long, Boolean> hasContextExampleMap
    ) {
    }

    private record GeneratedPlan(
            TrainingPlanEntity plan,
            List<TrainingPlanItemEntity> planItems
    ) {
    }
}
