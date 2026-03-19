package com.huashi.eftransfer.app.modules.training.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.audit.service.AuditLogService;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairExampleEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairSenseEntity;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairExampleMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairSenseMapper;
import com.huashi.eftransfer.app.modules.training.dto.StartTrainingSessionRequest;
import com.huashi.eftransfer.app.modules.training.dto.SubmitTrainingAnswerRequest;
import com.huashi.eftransfer.app.modules.training.entity.ReviewScheduleEntity;
import com.huashi.eftransfer.app.modules.training.entity.TrainingItemResultEntity;
import com.huashi.eftransfer.app.modules.training.entity.TrainingPlanEntity;
import com.huashi.eftransfer.app.modules.training.entity.TrainingPlanItemEntity;
import com.huashi.eftransfer.app.modules.training.entity.TrainingSessionEntity;
import com.huashi.eftransfer.app.modules.training.entity.WrongBookEntity;
import com.huashi.eftransfer.app.modules.training.event.TrainingCompletedEvent;
import com.huashi.eftransfer.app.modules.training.event.TrainingCompletedEventPublisher;
import com.huashi.eftransfer.app.modules.training.mapper.ReviewScheduleMapper;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingItemResultMapper;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingPlanItemMapper;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingPlanMapper;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingSessionMapper;
import com.huashi.eftransfer.app.modules.training.mapper.WrongBookMapper;
import com.huashi.eftransfer.app.modules.training.support.TrainingLearningProfileSnapshot;
import com.huashi.eftransfer.app.modules.training.support.TrainingOptionPayload;
import com.huashi.eftransfer.app.modules.training.support.TrainingRiskWordSnapshot;
import com.huashi.eftransfer.app.modules.training.support.TrainingSessionSummarySnapshot;
import com.huashi.eftransfer.app.modules.training.support.TrainingStimulusPayload;
import com.huashi.eftransfer.app.modules.training.vo.TrainingNextItemVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingExerciseContentVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingOptionViewVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingQuestionItemVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingRiskWordVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingSessionCreatedVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingSessionProgressVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingSessionSummaryVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingWordPairVO;
import com.huashi.eftransfer.app.modules.user.entity.StudentProfileEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.LexicalPairType;
import com.huashi.eftransfer.shared.enums.ReviewScheduleStatus;
import com.huashi.eftransfer.shared.enums.RiskLevel;
import com.huashi.eftransfer.shared.enums.TrainingAnswerState;
import com.huashi.eftransfer.shared.enums.TrainingCognitiveTag;
import com.huashi.eftransfer.shared.enums.TrainingMode;
import com.huashi.eftransfer.shared.enums.TrainingPlanStatus;
import com.huashi.eftransfer.shared.enums.TrainingSessionStatus;
import com.huashi.eftransfer.shared.enums.WrongBookMasteryStatus;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TrainingSessionService {

    private static final Logger log = LoggerFactory.getLogger(TrainingSessionService.class);
    private static final List<Integer> REVIEW_INTERVAL_DAYS = List.of(1, 3, 7, 14);
    private static final int SLOW_REACTION_THRESHOLD_MS = 1200;

    private final TrainingPlanMapper trainingPlanMapper;
    private final TrainingPlanItemMapper trainingPlanItemMapper;
    private final TrainingSessionMapper trainingSessionMapper;
    private final TrainingItemResultMapper trainingItemResultMapper;
    private final WrongBookMapper wrongBookMapper;
    private final ReviewScheduleMapper reviewScheduleMapper;
    private final LexicalPairMapper lexicalPairMapper;
    private final LexicalPairSenseMapper lexicalPairSenseMapper;
    private final LexicalPairExampleMapper lexicalPairExampleMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final com.huashi.eftransfer.app.modules.training.support.TrainingJsonCodec trainingJsonCodec;
    private final TrainingItemGenerator trainingItemGenerator;
    private final TrainingCompletedEventPublisher trainingCompletedEventPublisher;
    private final AuditLogService auditLogService;

    public TrainingSessionService(
            TrainingPlanMapper trainingPlanMapper,
            TrainingPlanItemMapper trainingPlanItemMapper,
            TrainingSessionMapper trainingSessionMapper,
            TrainingItemResultMapper trainingItemResultMapper,
            WrongBookMapper wrongBookMapper,
            ReviewScheduleMapper reviewScheduleMapper,
            LexicalPairMapper lexicalPairMapper,
            LexicalPairSenseMapper lexicalPairSenseMapper,
            LexicalPairExampleMapper lexicalPairExampleMapper,
            StudentProfileMapper studentProfileMapper,
            com.huashi.eftransfer.app.modules.training.support.TrainingJsonCodec trainingJsonCodec,
            TrainingItemGenerator trainingItemGenerator,
            TrainingCompletedEventPublisher trainingCompletedEventPublisher,
            AuditLogService auditLogService
    ) {
        this.trainingPlanMapper = trainingPlanMapper;
        this.trainingPlanItemMapper = trainingPlanItemMapper;
        this.trainingSessionMapper = trainingSessionMapper;
        this.trainingItemResultMapper = trainingItemResultMapper;
        this.wrongBookMapper = wrongBookMapper;
        this.reviewScheduleMapper = reviewScheduleMapper;
        this.lexicalPairMapper = lexicalPairMapper;
        this.lexicalPairSenseMapper = lexicalPairSenseMapper;
        this.lexicalPairExampleMapper = lexicalPairExampleMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.trainingJsonCodec = trainingJsonCodec;
        this.trainingItemGenerator = trainingItemGenerator;
        this.trainingCompletedEventPublisher = trainingCompletedEventPublisher;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public TrainingSessionCreatedVO startSession(StartTrainingSessionRequest request) {
        TrainingMode mode = parseTrainingMode(request.mode());
        TrainingPlanEntity plan = requireAccessiblePlan(request.planId());
        List<TrainingPlanItemEntity> planItems = loadPlanItems(plan.getId(), mode);
        if (planItems.isEmpty()) {
            throw new BusinessException(ResultCode.CONFLICT, "Training plan does not contain items for the requested mode", 409);
        }

        long sessionSeed = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        int targetVolume = targetVolume(plan.getRiskLevel());
        List<SessionPlanItem> sessionPlanItems = expandPlanItems(planItems, targetVolume);

        TrainingSessionEntity session = new TrainingSessionEntity();
        session.setPlanId(plan.getId());
        session.setOwnerUserId(plan.getOwnerUserId());
        session.setMode(mode.name());
        session.setStatus(TrainingSessionStatus.IN_PROGRESS.name());
        session.setSessionSeed(sessionSeed);
        session.setTotalItems(sessionPlanItems.size());
        session.setAnsweredItems(0);
        session.setCurrentItemOrder(sessionPlanItems.isEmpty() ? null : 1);
        session.setPlannedDifficulty(plan.getRecommendedDifficulty());
        session.setRiskLevel(plan.getRiskLevel());
        session.setStartedAt(LocalDateTime.now());
        trainingSessionMapper.insert(session);

        Map<Long, LexicalPairEntity> pairMap = loadLexicalPairMap(sessionPlanItems.stream()
                .map(sessionPlanItem -> sessionPlanItem.planItem().getLexicalPairId())
                .toList());
        Map<Long, List<TrainingItemGenerator.SenseBundle>> senseBundleMap = loadSenseBundles(pairMap.keySet());

        int presentationOrder = 1;
        for (SessionPlanItem sessionPlanItem : sessionPlanItems) {
            TrainingPlanItemEntity planItem = sessionPlanItem.planItem();
            LexicalPairEntity pair = pairMap.get(planItem.getLexicalPairId());
            if (pair == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "Training plan references a missing lexical pair", 404);
            }
            TrainingItemGenerator.GeneratedItem generatedItem = trainingItemGenerator.generate(
                    new TrainingItemGenerator.GenerationContext(
                            planItem,
                            pair,
                            senseBundleMap.getOrDefault(planItem.getLexicalPairId(), List.of()),
                            sessionPlanItem.exposureIndex(),
                            sessionSeed + presentationOrder
                    )
            );

            TrainingItemResultEntity itemResult = new TrainingItemResultEntity();
            itemResult.setSessionId(session.getId());
            itemResult.setPlanItemId(planItem.getId());
            itemResult.setLexicalPairId(planItem.getLexicalPairId());
            itemResult.setMode(mode.name());
            itemResult.setItemType(generatedItem.itemType().name());
            itemResult.setPresentationOrder(presentationOrder++);
            itemResult.setAnswerState(TrainingAnswerState.PENDING.name());
            itemResult.setCognitiveTag(generatedItem.cognitiveTag().name());
            itemResult.setStimulusJson(trainingJsonCodec.write(generatedItem.stimulus()));
            itemResult.setOptionsJson(trainingJsonCodec.write(generatedItem.options()));
            itemResult.setCorrectAnswerKey(generatedItem.correctAnswerKey());
            itemResult.setReviewRequired(Boolean.FALSE);
            trainingItemResultMapper.insert(itemResult);
        }

        plan.setStatus(TrainingPlanStatus.STARTED.name());
        if (plan.getStartedAt() == null) {
            plan.setStartedAt(LocalDateTime.now());
        }
        trainingPlanMapper.updateById(plan);

        auditLogService.record("start_training_session", "training_session", String.valueOf(session.getId()), request, ResultCode.SUCCESS.code());
        log.info("event=training_session_started sessionId={} planId={} ownerUserId={} mode={} totalItems={}",
                session.getId(), plan.getId(), plan.getOwnerUserId(), mode.name(), session.getTotalItems());
        return new TrainingSessionCreatedVO(
                session.getId(),
                session.getPlanId(),
                session.getStatus(),
                session.getMode(),
                session.getTotalItems(),
                session.getAnsweredItems(),
                session.getCurrentItemOrder()
        );
    }

    public TrainingNextItemVO getNextItem(Long sessionId) {
        TrainingSessionEntity session = requireAccessibleSession(sessionId);
        if (!TrainingSessionStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            return new TrainingNextItemVO(
                    session.getId(),
                    session.getStatus(),
                    session.getMode(),
                    session.getTotalItems(),
                    session.getAnsweredItems(),
                    session.getCurrentItemOrder(),
                    false,
                    null
            );
        }

        TrainingItemResultEntity itemResult = trainingItemResultMapper.selectOne(Wrappers.<TrainingItemResultEntity>lambdaQuery()
                .eq(TrainingItemResultEntity::getSessionId, sessionId)
                .eq(TrainingItemResultEntity::getAnswerState, TrainingAnswerState.PENDING.name())
                .orderByAsc(TrainingItemResultEntity::getPresentationOrder)
                .last("LIMIT 1"));
        if (itemResult == null) {
            return new TrainingNextItemVO(
                    session.getId(),
                    session.getStatus(),
                    session.getMode(),
                    session.getTotalItems(),
                    session.getAnsweredItems(),
                    session.getCurrentItemOrder(),
                    false,
                    null
            );
        }

        TrainingPlanItemEntity planItem = trainingPlanItemMapper.selectById(itemResult.getPlanItemId());
        LexicalPairEntity pair = lexicalPairMapper.selectById(itemResult.getLexicalPairId());
        TrainingStimulusPayload stimulus = trainingJsonCodec.readStimulus(itemResult.getStimulusJson());
        List<TrainingOptionViewVO> options = trainingJsonCodec.readOptions(itemResult.getOptionsJson()).stream()
                .map(option -> new TrainingOptionViewVO(option.key(), option.label()))
                .toList();

        return new TrainingNextItemVO(
                session.getId(),
                session.getStatus(),
                session.getMode(),
                session.getTotalItems(),
                session.getAnsweredItems(),
                itemResult.getPresentationOrder(),
                true,
                new TrainingQuestionItemVO(
                        itemResult.getId(),
                        itemResult.getPlanItemId(),
                        itemResult.getMode(),
                        itemResult.getItemType(),
                        itemResult.getPresentationOrder(),
                        itemResult.getLexicalPairId(),
                        pair == null ? null : pair.getEnglishWord(),
                        pair == null ? null : pair.getFrenchWord(),
                        pair == null ? null : pair.getChineseGloss(),
                        pair == null ? null : pair.getLexicalPairType(),
                        pair == null ? null : new TrainingWordPairVO(
                                pair.getEnglishWord(),
                                pair.getFrenchWord(),
                                pair.getChineseGloss(),
                                frontendPairType(pair.getLexicalPairType())
                        ),
                        planItem == null ? null : planItem.getRecommendedDifficulty(),
                        itemResult.getCognitiveTag(),
                        new TrainingExerciseContentVO(
                                stimulus.questionText(),
                                options.stream().map(TrainingOptionViewVO::label).toList(),
                                stimulus.explanation(),
                                stimulus.contextSupportLevel(),
                                stimulus.contextSentence()
                        ),
                        stimulus,
                        options
                )
        );
    }

    @Transactional
    public TrainingSessionProgressVO submitAnswer(Long sessionId, SubmitTrainingAnswerRequest request) {
        TrainingSessionEntity session = requireInProgressSession(sessionId);
        TrainingItemResultEntity itemResult = requirePendingItemResult(sessionId, request.itemResultId());
        List<TrainingOptionPayload> options = trainingJsonCodec.readOptions(itemResult.getOptionsJson());
        boolean optionExists = options.stream().anyMatch(option -> option.key().equalsIgnoreCase(request.selectedAnswerKey()));
        if (!optionExists) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Selected answer key is not defined in this training item", 400);
        }
        LexicalPairEntity pair = requireLexicalPair(itemResult.getLexicalPairId());
        boolean correct = itemResult.getCorrectAnswerKey().equalsIgnoreCase(request.selectedAnswerKey());
        String errorType = resolveErrorType(itemResult.getMode(), pair.getLexicalPairType(), correct);
        String adaptationAction = resolveAdaptationAction(itemResult.getMode(), pair.getLexicalPairType(), correct, request.reactionTimeMs(), errorType);

        itemResult.setSelectedAnswerKey(request.selectedAnswerKey());
        itemResult.setAnswerPayloadJson(trainingJsonCodec.write(Map.of(
                "selectedAnswerKey", request.selectedAnswerKey(),
                "reactionTimeMs", request.reactionTimeMs(),
                "hesitationTimeMs", request.hesitationTimeMs()
        )));
        itemResult.setSubmittedAt(LocalDateTime.now());
        itemResult.setReactionTimeMs(request.reactionTimeMs());
        itemResult.setHesitationTimeMs(request.hesitationTimeMs());
        itemResult.setIsCorrect(correct);
        itemResult.setDetectedErrorType(errorType);
        itemResult.setReviewRequired(!correct);
        itemResult.setAdaptationAction(adaptationAction);
        itemResult.setAnswerState(TrainingAnswerState.ANSWERED.name());
        trainingItemResultMapper.updateById(itemResult);

        session.setAnsweredItems(session.getAnsweredItems() + 1);
        TrainingItemResultEntity nextPending = trainingItemResultMapper.selectOne(Wrappers.<TrainingItemResultEntity>lambdaQuery()
                .eq(TrainingItemResultEntity::getSessionId, sessionId)
                .eq(TrainingItemResultEntity::getAnswerState, TrainingAnswerState.PENDING.name())
                .orderByAsc(TrainingItemResultEntity::getPresentationOrder)
                .last("LIMIT 1"));
        session.setCurrentItemOrder(nextPending == null ? session.getTotalItems() : nextPending.getPresentationOrder());
        trainingSessionMapper.updateById(session);

        auditLogService.record("submit_training_answer", "training_session", String.valueOf(sessionId), request, ResultCode.SUCCESS.code());
        return progressVO(session);
    }

    @Transactional
    public TrainingSessionProgressVO completeSession(Long sessionId) {
        TrainingSessionEntity session = requireInProgressSession(sessionId);
        if (!Objects.equals(session.getAnsweredItems(), session.getTotalItems())) {
            throw new BusinessException(ResultCode.CONFLICT, "Training session still has unanswered items", 409);
        }

        List<TrainingItemResultEntity> itemResults = listSessionItems(sessionId);
        Map<Long, LexicalPairEntity> pairMap = loadLexicalPairMap(itemResults.stream().map(TrainingItemResultEntity::getLexicalPairId).toList());
        Map<Long, List<TrainingItemResultEntity>> pairResultMap = itemResults.stream()
                .collect(Collectors.groupingBy(TrainingItemResultEntity::getLexicalPairId, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<Long, List<TrainingItemResultEntity>> entry : pairResultMap.entrySet()) {
            LexicalPairEntity pair = pairMap.get(entry.getKey());
            if (pair == null) {
                continue;
            }
            List<TrainingItemResultEntity> results = entry.getValue();
            boolean hasIncorrect = results.stream().anyMatch(result -> Boolean.FALSE.equals(result.getIsCorrect()));
            if (hasIncorrect) {
                TrainingItemResultEntity latestIncorrect = results.stream()
                        .filter(result -> Boolean.FALSE.equals(result.getIsCorrect()))
                        .max(Comparator.comparing(TrainingItemResultEntity::getPresentationOrder))
                        .orElseThrow();
                upsertWrongBookAndReviewSchedule(session, pair, latestIncorrect);
            } else {
                advanceReviewIfApplicable(session, pair);
            }
        }

        TrainingSessionSummarySnapshot summarySnapshot = buildSummarySnapshot(session, itemResults, pairMap);
        session.setStatus(TrainingSessionStatus.COMPLETED.name());
        session.setCompletedAt(LocalDateTime.now());
        session.setCurrentItemOrder(session.getTotalItems());
        session.setSummarySnapshotJson(trainingJsonCodec.write(summarySnapshot));
        trainingSessionMapper.updateById(session);

        TrainingPlanEntity plan = trainingPlanMapper.selectById(session.getPlanId());
        if (plan != null) {
            plan.setStatus(TrainingPlanStatus.COMPLETED.name());
            plan.setCompletedAt(LocalDateTime.now());
            trainingPlanMapper.updateById(plan);
            updateLearningProfile(plan, session, summarySnapshot);
        }
        registerAfterCommitEvent(session, summarySnapshot);

        auditLogService.record("complete_training_session", "training_session", String.valueOf(sessionId), Map.of("sessionId", sessionId), ResultCode.SUCCESS.code());
        log.info("event=training_session_completed sessionId={} planId={} ownerUserId={} accuracy={} averageReactionTime={}",
                session.getId(), session.getPlanId(), session.getOwnerUserId(), summarySnapshot.accuracy(), summarySnapshot.averageReactionTime());
        return progressVO(session);
    }

    public TrainingSessionSummaryVO getSummary(Long sessionId) {
        TrainingSessionEntity session = requireAccessibleSession(sessionId);
        if (!TrainingSessionStatus.COMPLETED.name().equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Training session is not completed", 409);
        }
        TrainingSessionSummarySnapshot summarySnapshot = trainingJsonCodec.readSummarySnapshot(session.getSummarySnapshotJson());
        return new TrainingSessionSummaryVO(
                session.getId(),
                session.getMode(),
                summarySnapshot.accuracy(),
                summarySnapshot.averageReactionTime(),
                summarySnapshot.improvementHint(),
                summarySnapshot.nextRecommendedMode(),
                summarySnapshot.riskWordsToReview().stream()
                        .map(this::toRiskWordVO)
                        .toList()
        );
    }

    private TrainingSessionSummarySnapshot buildSummarySnapshot(
            TrainingSessionEntity session,
            List<TrainingItemResultEntity> itemResults,
            Map<Long, LexicalPairEntity> pairMap
    ) {
        double accuracy = itemResults.isEmpty()
                ? 0
                : itemResults.stream().filter(result -> Boolean.TRUE.equals(result.getIsCorrect())).count() / (double) itemResults.size();
        long averageReactionTime = Math.round(itemResults.stream()
                .filter(result -> result.getReactionTimeMs() != null)
                .mapToInt(TrainingItemResultEntity::getReactionTimeMs)
                .average()
                .orElse(0));
        String nextRecommendedMode = determineNextRecommendedMode(itemResults, pairMap).name();
        String improvementHint = buildImprovementHint(itemResults, accuracy, averageReactionTime);
        List<TrainingRiskWordSnapshot> riskWordsToReview = loadRiskWordsToReview(session.getOwnerUserId(), pairMap.keySet());
        return new TrainingSessionSummarySnapshot(accuracy, averageReactionTime, improvementHint, nextRecommendedMode, riskWordsToReview);
    }

    private List<TrainingRiskWordSnapshot> loadRiskWordsToReview(Long ownerUserId, Collection<Long> sessionPairIds) {
        List<WrongBookEntity> wrongBooks = wrongBookMapper.selectList(Wrappers.<WrongBookEntity>lambdaQuery()
                .eq(WrongBookEntity::getOwnerUserId, ownerUserId)
                .in(!sessionPairIds.isEmpty(), WrongBookEntity::getLexicalPairId, sessionPairIds)
                .orderByDesc(WrongBookEntity::getWrongCount)
                .orderByAsc(WrongBookEntity::getNextReviewAt)
                .orderByDesc(WrongBookEntity::getLastWrongAt)
                .last("LIMIT 5"));
        Map<Long, LexicalPairEntity> pairMap = loadLexicalPairMap(wrongBooks.stream().map(WrongBookEntity::getLexicalPairId).toList());
        return wrongBooks.stream()
                .map(wrongBook -> {
                    LexicalPairEntity pair = pairMap.get(wrongBook.getLexicalPairId());
                    return new TrainingRiskWordSnapshot(
                            wrongBook.getLexicalPairId(),
                            pair == null ? null : pair.getEnglishWord(),
                            pair == null ? null : pair.getFrenchWord(),
                            pair == null ? null : pair.getChineseGloss(),
                            pair == null ? null : pair.getLexicalPairType(),
                            wrongBook.getLastErrorType() == null ? "需要复习该风险词对" : "最近主要风险：" + wrongBook.getLastErrorType(),
                            resolveRiskLevel(wrongBook.getWrongCount()).name(),
                            wrongBook.getLastErrorType()
                    );
                })
                .toList();
    }

    private void upsertWrongBookAndReviewSchedule(
            TrainingSessionEntity session,
            LexicalPairEntity pair,
            TrainingItemResultEntity latestIncorrect
    ) {
        LocalDateTime now = LocalDateTime.now();
        WrongBookEntity wrongBook = wrongBookMapper.selectOne(Wrappers.<WrongBookEntity>lambdaQuery()
                .eq(WrongBookEntity::getOwnerUserId, session.getOwnerUserId())
                .eq(WrongBookEntity::getLexicalPairId, pair.getId()));

        if (wrongBook == null) {
            wrongBook = new WrongBookEntity();
            wrongBook.setOwnerUserId(session.getOwnerUserId());
            wrongBook.setLexicalPairId(pair.getId());
            wrongBook.setSourceTrainingSessionId(session.getId());
            wrongBook.setSourceItemResultId(latestIncorrect.getId());
            wrongBook.setWrongCount(1);
            wrongBook.setFirstWrongAt(now);
            wrongBook.setLastWrongAt(now);
            wrongBook.setLastErrorType(latestIncorrect.getDetectedErrorType());
            wrongBook.setMasteryStatus(WrongBookMasteryStatus.REVIEWING.name());
            wrongBook.setLatestSnapshotJson(trainingJsonCodec.write(new TrainingRiskWordSnapshot(
                    pair.getId(),
                    pair.getEnglishWord(),
                    pair.getFrenchWord(),
                    pair.getChineseGloss(),
                    pair.getLexicalPairType(),
                    "首次进入错题本",
                    RiskLevel.MEDIUM.name(),
                    latestIncorrect.getDetectedErrorType()
            )));
            wrongBookMapper.insert(wrongBook);
        } else {
            wrongBook.setSourceTrainingSessionId(session.getId());
            wrongBook.setSourceItemResultId(latestIncorrect.getId());
            wrongBook.setWrongCount(wrongBook.getWrongCount() + 1);
            wrongBook.setLastWrongAt(now);
            wrongBook.setLastErrorType(latestIncorrect.getDetectedErrorType());
            wrongBook.setMasteryStatus(WrongBookMasteryStatus.ACTIVE.name());
            wrongBook.setLatestSnapshotJson(trainingJsonCodec.write(new TrainingRiskWordSnapshot(
                    pair.getId(),
                    pair.getEnglishWord(),
                    pair.getFrenchWord(),
                    pair.getChineseGloss(),
                    pair.getLexicalPairType(),
                    "重复错误，重新进入间隔复习",
                    resolveRiskLevel(wrongBook.getWrongCount()).name(),
                    latestIncorrect.getDetectedErrorType()
            )));
            wrongBookMapper.updateById(wrongBook);
        }

        reviewScheduleMapper.delete(Wrappers.<ReviewScheduleEntity>lambdaQuery()
                .eq(ReviewScheduleEntity::getWrongBookId, wrongBook.getId())
                .eq(ReviewScheduleEntity::getStatus, ReviewScheduleStatus.PENDING.name()));
        for (int index = 0; index < REVIEW_INTERVAL_DAYS.size(); index++) {
            Integer intervalDays = REVIEW_INTERVAL_DAYS.get(index);
            ReviewScheduleEntity schedule = new ReviewScheduleEntity();
            schedule.setOwnerUserId(session.getOwnerUserId());
            schedule.setLexicalPairId(pair.getId());
            schedule.setWrongBookId(wrongBook.getId());
            schedule.setSourceTrainingSessionId(session.getId());
            schedule.setScheduleStage(index + 1);
            schedule.setIntervalDays(intervalDays);
            schedule.setDueAt(now.plusDays(intervalDays));
            schedule.setStatus(ReviewScheduleStatus.PENDING.name());
            schedule.setReviewMode(resolveReviewMode(session.getMode(), pair.getLexicalPairType()).name());
            schedule.setTriggerReason("Repeated training error");
            schedule.setSnapshotJson(trainingJsonCodec.write(Map.of(
                    "pairId", pair.getId(),
                    "sourceTrainingSessionId", session.getId(),
                    "detectedErrorType", latestIncorrect.getDetectedErrorType()
            )));
            reviewScheduleMapper.insert(schedule);
            if (index == 0) {
                wrongBook.setNextReviewAt(schedule.getDueAt());
            }
        }
        wrongBookMapper.updateById(wrongBook);
    }

    private void advanceReviewIfApplicable(TrainingSessionEntity session, LexicalPairEntity pair) {
        WrongBookEntity wrongBook = wrongBookMapper.selectOne(Wrappers.<WrongBookEntity>lambdaQuery()
                .eq(WrongBookEntity::getOwnerUserId, session.getOwnerUserId())
                .eq(WrongBookEntity::getLexicalPairId, pair.getId()));
        if (wrongBook == null) {
            return;
        }
        ReviewScheduleEntity pending = reviewScheduleMapper.selectOne(Wrappers.<ReviewScheduleEntity>lambdaQuery()
                .eq(ReviewScheduleEntity::getOwnerUserId, session.getOwnerUserId())
                .eq(ReviewScheduleEntity::getLexicalPairId, pair.getId())
                .eq(ReviewScheduleEntity::getStatus, ReviewScheduleStatus.PENDING.name())
                .orderByAsc(ReviewScheduleEntity::getDueAt)
                .orderByAsc(ReviewScheduleEntity::getId)
                .last("LIMIT 1"));
        if (pending == null) {
            wrongBook.setMasteryStatus(WrongBookMasteryStatus.MASTERED.name());
            wrongBook.setNextReviewAt(null);
            wrongBookMapper.updateById(wrongBook);
            return;
        }

        pending.setStatus(ReviewScheduleStatus.COMPLETED.name());
        pending.setCompletedAt(LocalDateTime.now());
        reviewScheduleMapper.updateById(pending);

        ReviewScheduleEntity nextPending = reviewScheduleMapper.selectOne(Wrappers.<ReviewScheduleEntity>lambdaQuery()
                .eq(ReviewScheduleEntity::getWrongBookId, wrongBook.getId())
                .eq(ReviewScheduleEntity::getStatus, ReviewScheduleStatus.PENDING.name())
                .orderByAsc(ReviewScheduleEntity::getDueAt)
                .orderByAsc(ReviewScheduleEntity::getId)
                .last("LIMIT 1"));
        wrongBook.setMasteryStatus(nextPending == null ? WrongBookMasteryStatus.MASTERED.name() : WrongBookMasteryStatus.REVIEWING.name());
        wrongBook.setNextReviewAt(nextPending == null ? null : nextPending.getDueAt());
        wrongBookMapper.updateById(wrongBook);
    }

    private void updateLearningProfile(
            TrainingPlanEntity plan,
            TrainingSessionEntity session,
            TrainingSessionSummarySnapshot summarySnapshot
    ) {
        StudentProfileEntity studentProfile = studentProfileMapper.selectOne(Wrappers.<StudentProfileEntity>lambdaQuery()
                .eq(StudentProfileEntity::getUserId, session.getOwnerUserId()));
        if (studentProfile == null) {
            return;
        }
        long pendingReviewCount = reviewScheduleMapper.selectCount(Wrappers.<ReviewScheduleEntity>lambdaQuery()
                .eq(ReviewScheduleEntity::getOwnerUserId, session.getOwnerUserId())
                .eq(ReviewScheduleEntity::getStatus, ReviewScheduleStatus.PENDING.name()));
        List<String> modeWeaknesses = deriveModeWeaknesses(summarySnapshot, pendingReviewCount);
        TrainingLearningProfileSnapshot learningProfileSnapshot = new TrainingLearningProfileSnapshot(
                plan.getSourceDiagnosisSummaryId(),
                session.getId(),
                summarySnapshot.nextRecommendedMode(),
                modeWeaknesses,
                summarySnapshot.riskWordsToReview(),
                Math.toIntExact(pendingReviewCount),
                summarySnapshot.accuracy(),
                summarySnapshot.averageReactionTime(),
                LocalDateTime.now()
        );
        studentProfile.setLearningProfileSnapshotJson(trainingJsonCodec.write(learningProfileSnapshot));
        studentProfile.setLearningProfileUpdatedAt(LocalDateTime.now());
        studentProfileMapper.updateById(studentProfile);
    }

    private void registerAfterCommitEvent(
            TrainingSessionEntity session,
            TrainingSessionSummarySnapshot summarySnapshot
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishCompletedEvent(session, summarySnapshot);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishCompletedEvent(session, summarySnapshot);
            }
        });
    }

    private void publishCompletedEvent(
            TrainingSessionEntity session,
            TrainingSessionSummarySnapshot summarySnapshot
    ) {
        int pendingReviewCount = Math.toIntExact(reviewScheduleMapper.selectCount(Wrappers.<ReviewScheduleEntity>lambdaQuery()
                .eq(ReviewScheduleEntity::getOwnerUserId, session.getOwnerUserId())
                .eq(ReviewScheduleEntity::getStatus, ReviewScheduleStatus.PENDING.name())));
        trainingCompletedEventPublisher.publish(new TrainingCompletedEvent(
                session.getId(),
                session.getPlanId(),
                session.getOwnerUserId(),
                session.getMode(),
                session.getCompletedAt(),
                summarySnapshot.accuracy(),
                summarySnapshot.averageReactionTime(),
                summarySnapshot.nextRecommendedMode(),
                pendingReviewCount,
                MDC.get("traceId"),
                1
        ));
    }

    private List<String> deriveModeWeaknesses(TrainingSessionSummarySnapshot summarySnapshot, long pendingReviewCount) {
        List<String> weaknesses = new ArrayList<>();
        if (pendingReviewCount > 0) {
            weaknesses.add("spaced_review_backlog");
        }
        if (TrainingMode.FALSE_FRIEND_DISCRIM.name().equals(summarySnapshot.nextRecommendedMode())) {
            weaknesses.add("false_friend_control");
        }
        if (TrainingMode.CONTEXT_FIX.name().equals(summarySnapshot.nextRecommendedMode())) {
            weaknesses.add("context_locking");
        }
        if (TrainingMode.SPEED_CHALLENGE.name().equals(summarySnapshot.nextRecommendedMode())) {
            weaknesses.add("rapid_recognition");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("stability_consolidation");
        }
        return weaknesses;
    }

    private TrainingMode determineNextRecommendedMode(
            List<TrainingItemResultEntity> itemResults,
            Map<Long, LexicalPairEntity> pairMap
    ) {
        long falseFriendErrors = itemResults.stream()
                .filter(result -> Boolean.FALSE.equals(result.getIsCorrect()))
                .filter(result -> isFalseFriendLike(pairMap.get(result.getLexicalPairId())))
                .count();
        long contextErrors = itemResults.stream()
                .map(TrainingItemResultEntity::getDetectedErrorType)
                .filter(Objects::nonNull)
                .filter(value -> value.equalsIgnoreCase("CONTEXT_IGNORED"))
                .count();
        long slowCorrect = itemResults.stream()
                .filter(result -> Boolean.TRUE.equals(result.getIsCorrect()))
                .filter(result -> result.getReactionTimeMs() != null && result.getReactionTimeMs() >= SLOW_REACTION_THRESHOLD_MS)
                .count();
        if (falseFriendErrors > 0) {
            return TrainingMode.FALSE_FRIEND_DISCRIM;
        }
        if (contextErrors > 0) {
            return TrainingMode.CONTEXT_FIX;
        }
        if (slowCorrect > 0) {
            return TrainingMode.SPEED_CHALLENGE;
        }
        return TrainingMode.COGNATE_BOOST;
    }

    private String buildImprovementHint(List<TrainingItemResultEntity> itemResults, double accuracy, long averageReactionTime) {
        long falseFriendErrors = itemResults.stream()
                .map(TrainingItemResultEntity::getDetectedErrorType)
                .filter(Objects::nonNull)
                .filter(value -> value.equalsIgnoreCase("FALSE_FRIEND_CONFUSION") || value.equalsIgnoreCase("ORTHOGRAPHIC_INTERFERENCE"))
                .count();
        long contextErrors = itemResults.stream()
                .map(TrainingItemResultEntity::getDetectedErrorType)
                .filter(Objects::nonNull)
                .filter(value -> value.equalsIgnoreCase("CONTEXT_IGNORED"))
                .count();
        if (falseFriendErrors > 0) {
            return "继续优先做 false friend / 形近干扰辨析，先压住错误再追求速度。";
        }
        if (contextErrors > 0) {
            return "下一轮先完整读取语境，再锁定词义，避免只凭字形作答。";
        }
        if (accuracy >= 0.70 && averageReactionTime >= SLOW_REACTION_THRESHOLD_MS) {
            return "正确率已经具备，下一轮重点压缩反应时，建立更快的自动化识别。";
        }
        if (accuracy >= 0.85) {
            return "当前稳定度不错，可以继续推进更高难度或更快节奏训练。";
        }
        return "先保持稳定辨析，再逐步扩大训练量，重点回看本轮风险词。";
    }

    private TrainingRiskWordVO toRiskWordVO(TrainingRiskWordSnapshot snapshot) {
        return new TrainingRiskWordVO(
                snapshot.lexicalPairId(),
                snapshot.englishWord(),
                snapshot.frenchWord(),
                snapshot.chineseGloss(),
                snapshot.lexicalPairType(),
                snapshot.reason(),
                snapshot.riskLevel(),
                snapshot.dominantErrorType()
        );
    }

    private TrainingPlanEntity requireAccessiblePlan(Long planId) {
        TrainingPlanEntity plan = trainingPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Training plan was not found", 404);
        }
        if (!isAdmin() && !Objects.equals(plan.getOwnerUserId(), currentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have permission to access this training plan", 403);
        }
        return plan;
    }

    private TrainingSessionEntity requireAccessibleSession(Long sessionId) {
        TrainingSessionEntity session = trainingSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Training session was not found", 404);
        }
        if (!isAdmin() && !Objects.equals(session.getOwnerUserId(), currentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have permission to access this training session", 403);
        }
        return session;
    }

    private TrainingSessionEntity requireInProgressSession(Long sessionId) {
        TrainingSessionEntity session = requireAccessibleSession(sessionId);
        if (!TrainingSessionStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Training session is not in progress", 409);
        }
        return session;
    }

    private TrainingItemResultEntity requirePendingItemResult(Long sessionId, Long itemResultId) {
        TrainingItemResultEntity itemResult = trainingItemResultMapper.selectOne(Wrappers.<TrainingItemResultEntity>lambdaQuery()
                .eq(TrainingItemResultEntity::getId, itemResultId)
                .eq(TrainingItemResultEntity::getSessionId, sessionId));
        if (itemResult == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Training item result was not found", 404);
        }
        if (!TrainingAnswerState.PENDING.name().equals(itemResult.getAnswerState())) {
            throw new BusinessException(ResultCode.CONFLICT, "Training item has already been answered", 409);
        }
        return itemResult;
    }

    private List<TrainingPlanItemEntity> loadPlanItems(Long planId, TrainingMode mode) {
        List<TrainingPlanItemEntity> filtered = trainingPlanItemMapper.selectList(Wrappers.<TrainingPlanItemEntity>lambdaQuery()
                .eq(TrainingPlanItemEntity::getPlanId, planId)
                .eq(TrainingPlanItemEntity::getRecommendedMode, mode.name())
                .orderByDesc(TrainingPlanItemEntity::getPriorityScore)
                .orderByAsc(TrainingPlanItemEntity::getSortOrder)
                .orderByAsc(TrainingPlanItemEntity::getId));
        if (!filtered.isEmpty()) {
            return filtered;
        }
        return trainingPlanItemMapper.selectList(Wrappers.<TrainingPlanItemEntity>lambdaQuery()
                .eq(TrainingPlanItemEntity::getPlanId, planId)
                .orderByDesc(TrainingPlanItemEntity::getPriorityScore)
                .orderByAsc(TrainingPlanItemEntity::getSortOrder)
                .orderByAsc(TrainingPlanItemEntity::getId));
    }

    private List<SessionPlanItem> expandPlanItems(List<TrainingPlanItemEntity> planItems, int targetVolume) {
        List<TrainingPlanItemEntity> sortedPlanItems = planItems.stream()
                .sorted(Comparator.comparing(TrainingPlanItemEntity::getPriorityScore).reversed()
                        .thenComparing(TrainingPlanItemEntity::getSortOrder)
                        .thenComparing(TrainingPlanItemEntity::getId))
                .toList();
        int maxExposure = sortedPlanItems.stream().mapToInt(TrainingPlanItemEntity::getExpectedExposures).max().orElse(1);
        List<SessionPlanItem> expanded = new ArrayList<>();
        for (int round = 1; round <= maxExposure && expanded.size() < targetVolume; round++) {
            for (TrainingPlanItemEntity planItem : sortedPlanItems) {
                if (planItem.getExpectedExposures() >= round) {
                    expanded.add(new SessionPlanItem(planItem, round));
                    if (expanded.size() >= targetVolume) {
                        break;
                    }
                }
            }
        }
        int round = maxExposure + 1;
        while (expanded.size() < targetVolume) {
            for (TrainingPlanItemEntity planItem : sortedPlanItems) {
                expanded.add(new SessionPlanItem(planItem, round));
                if (expanded.size() >= targetVolume) {
                    break;
                }
            }
            round++;
        }
        return expanded;
    }

    private Map<Long, List<TrainingItemGenerator.SenseBundle>> loadSenseBundles(Collection<Long> pairIds) {
        if (pairIds.isEmpty()) {
            return Map.of();
        }
        List<LexicalPairSenseEntity> senses = lexicalPairSenseMapper.selectList(Wrappers.<LexicalPairSenseEntity>lambdaQuery()
                .in(LexicalPairSenseEntity::getLexicalPairId, pairIds)
                .orderByAsc(LexicalPairSenseEntity::getSortOrder)
                .orderByAsc(LexicalPairSenseEntity::getId));
        Map<Long, List<LexicalPairExampleEntity>> exampleMap = loadExampleMap(senses.stream().map(LexicalPairSenseEntity::getId).toList());
        return senses.stream()
                .collect(Collectors.groupingBy(LexicalPairSenseEntity::getLexicalPairId, LinkedHashMap::new, Collectors.mapping(
                        sense -> new TrainingItemGenerator.SenseBundle(sense, exampleMap.getOrDefault(sense.getId(), List.of())),
                        Collectors.toList()
                )));
    }

    private Map<Long, List<LexicalPairExampleEntity>> loadExampleMap(Collection<Long> senseIds) {
        if (senseIds.isEmpty()) {
            return Map.of();
        }
        return lexicalPairExampleMapper.selectList(Wrappers.<LexicalPairExampleEntity>lambdaQuery()
                        .in(LexicalPairExampleEntity::getLexicalPairSenseId, senseIds)
                        .orderByAsc(LexicalPairExampleEntity::getSortOrder)
                        .orderByAsc(LexicalPairExampleEntity::getId))
                .stream()
                .collect(Collectors.groupingBy(LexicalPairExampleEntity::getLexicalPairSenseId, LinkedHashMap::new, Collectors.toList()));
    }

    private List<TrainingItemResultEntity> listSessionItems(Long sessionId) {
        return trainingItemResultMapper.selectList(Wrappers.<TrainingItemResultEntity>lambdaQuery()
                .eq(TrainingItemResultEntity::getSessionId, sessionId)
                .orderByAsc(TrainingItemResultEntity::getPresentationOrder)
                .orderByAsc(TrainingItemResultEntity::getId));
    }

    private Map<Long, LexicalPairEntity> loadLexicalPairMap(Collection<Long> lexicalPairIds) {
        if (lexicalPairIds.isEmpty()) {
            return Map.of();
        }
        return lexicalPairMapper.selectBatchIds(new LinkedHashSet<>(lexicalPairIds))
                .stream()
                .collect(Collectors.toMap(LexicalPairEntity::getId, Function.identity()));
    }

    private LexicalPairEntity requireLexicalPair(Long lexicalPairId) {
        LexicalPairEntity pair = lexicalPairMapper.selectById(lexicalPairId);
        if (pair == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Lexical pair was not found", 404);
        }
        return pair;
    }

    private String resolveErrorType(String mode, String lexicalPairType, boolean correct) {
        if (correct) {
            return null;
        }
        TrainingMode trainingMode = TrainingMode.fromCode(mode);
        LexicalPairType pairType = LexicalPairType.fromCode(lexicalPairType);
        if (trainingMode == TrainingMode.CONTEXT_FIX) {
            return "CONTEXT_IGNORED";
        }
        if (pairType == LexicalPairType.FALSE_FRIEND) {
            return "FALSE_FRIEND_CONFUSION";
        }
        if (pairType == LexicalPairType.ORTHOGRAPHIC_SIMILAR) {
            return "ORTHOGRAPHIC_INTERFERENCE";
        }
        if (trainingMode == TrainingMode.COGNATE_BOOST) {
            return "UNDER_TRANSFER";
        }
        return "SEMANTIC_MISFIRE";
    }

    private String resolveAdaptationAction(String mode, String lexicalPairType, boolean correct, int reactionTimeMs, String errorType) {
        if (!correct && ("FALSE_FRIEND_CONFUSION".equalsIgnoreCase(errorType) || isFalseFriendLike(lexicalPairType))) {
            return "ESCALATE_FALSE_FRIEND";
        }
        if (!correct && "CONTEXT_IGNORED".equalsIgnoreCase(errorType)) {
            return "BOOST_CONTEXT";
        }
        if (correct && reactionTimeMs >= SLOW_REACTION_THRESHOLD_MS) {
            return "BOOST_SPEED";
        }
        if (!correct) {
            return "QUEUE_REVIEW";
        }
        return "KEEP_STABLE";
    }

    private TrainingMode resolveReviewMode(String sessionMode, String lexicalPairType) {
        if (isFalseFriendLike(lexicalPairType)) {
            return TrainingMode.FALSE_FRIEND_DISCRIM;
        }
        TrainingMode mode = TrainingMode.fromCode(sessionMode);
        if (mode == TrainingMode.CONTEXT_FIX) {
            return TrainingMode.CONTEXT_FIX;
        }
        if (mode == TrainingMode.SPEED_CHALLENGE) {
            return TrainingMode.SPEED_CHALLENGE;
        }
        return TrainingMode.COGNATE_BOOST;
    }

    private boolean isFalseFriendLike(LexicalPairEntity pair) {
        return pair != null && isFalseFriendLike(pair.getLexicalPairType());
    }

    private boolean isFalseFriendLike(String lexicalPairType) {
        if (lexicalPairType == null) {
            return false;
        }
        LexicalPairType pairType = LexicalPairType.fromCode(lexicalPairType);
        return pairType == LexicalPairType.FALSE_FRIEND || pairType == LexicalPairType.ORTHOGRAPHIC_SIMILAR;
    }

    private String frontendPairType(String lexicalPairType) {
        LexicalPairType pairType = LexicalPairType.fromCode(lexicalPairType);
        return switch (pairType) {
            case COGNATE -> "COGNATE";
            case FALSE_FRIEND -> "FALSE_FRIEND";
            case PARTIAL_COGNATE -> "PARTIAL";
            case ORTHOGRAPHIC_SIMILAR -> "ORTHOGRAPHIC";
        };
    }

    private RiskLevel resolveRiskLevel(int wrongCount) {
        if (wrongCount >= 3) {
            return RiskLevel.HIGH;
        }
        if (wrongCount >= 2) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private int targetVolume(String riskLevel) {
        RiskLevel level = RiskLevel.fromCode(riskLevel);
        return switch (level) {
            case HIGH, CRITICAL -> 12;
            case MEDIUM -> 10;
            case LOW -> 8;
        };
    }

    private TrainingMode parseTrainingMode(String value) {
        try {
            return TrainingMode.fromCode(value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported trainingMode: " + value, 400);
        }
    }

    private TrainingSessionProgressVO progressVO(TrainingSessionEntity session) {
        return new TrainingSessionProgressVO(
                session.getId(),
                session.getStatus(),
                session.getTotalItems(),
                session.getAnsweredItems(),
                session.getCurrentItemOrder(),
                TrainingSessionStatus.COMPLETED.name().equals(session.getStatus())
        );
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }

    private boolean isAdmin() {
        return SecurityUtils.getCurrentPrincipal()
                .map(principal -> principal.roles().contains("ADMIN"))
                .orElse(false);
    }

    private record SessionPlanItem(
            TrainingPlanItemEntity planItem,
            int exposureIndex
    ) {
    }
}
