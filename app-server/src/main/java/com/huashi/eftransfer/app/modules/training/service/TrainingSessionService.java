package com.huashi.eftransfer.app.modules.training.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.audit.service.AuditLogService;
import com.huashi.eftransfer.app.common.idempotency.IdempotencyService;
import com.huashi.eftransfer.app.common.session.SessionCompletionHookStatus;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.common.util.TokenGenerator;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairExampleEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairSenseEntity;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairExampleMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairMapper;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairSenseMapper;
import com.huashi.eftransfer.app.modules.training.dto.SaveTrainingProgressRequest;
import com.huashi.eftransfer.app.modules.training.dto.StartTrainingSessionRequest;
import com.huashi.eftransfer.app.modules.training.dto.SubmitTrainingAnswerRequest;
import com.huashi.eftransfer.app.modules.training.dto.TrainingSessionPageQuery;
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
import com.huashi.eftransfer.app.modules.training.support.TrainingSessionLaunchContext;
import com.huashi.eftransfer.app.modules.training.support.TrainingOptionPayload;
import com.huashi.eftransfer.app.modules.training.support.TrainingRiskWordSnapshot;
import com.huashi.eftransfer.app.modules.training.support.TrainingSessionSummarySnapshot;
import com.huashi.eftransfer.app.modules.training.support.TrainingStimulusPayload;
import com.huashi.eftransfer.app.modules.training.vo.TrainingNextItemVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingExerciseContentVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingHistorySummaryVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingItemResultDetailVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingOptionViewVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingQuestionItemVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingRiskWordVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingSessionCreatedVO;
import com.huashi.eftransfer.app.modules.training.vo.TrainingSessionHeartbeatVO;
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
import com.huashi.eftransfer.shared.enums.TrainingItemType;
import com.huashi.eftransfer.shared.enums.TrainingMode;
import com.huashi.eftransfer.shared.enums.TrainingPlanStatus;
import com.huashi.eftransfer.shared.enums.TrainingSessionStatus;
import com.huashi.eftransfer.shared.enums.WrongBookMasteryStatus;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageQuery;
import com.huashi.eftransfer.shared.page.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
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
    private final TrainingSessionCompletionService trainingSessionCompletionService;
    private final TrainingCompletedEventPublisher trainingCompletedEventPublisher;
    private final AuditLogService auditLogService;
    private final IdempotencyService idempotencyService;

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
            TrainingSessionCompletionService trainingSessionCompletionService,
            TrainingCompletedEventPublisher trainingCompletedEventPublisher,
            AuditLogService auditLogService,
            IdempotencyService idempotencyService
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
        this.trainingSessionCompletionService = trainingSessionCompletionService;
        this.trainingCompletedEventPublisher = trainingCompletedEventPublisher;
        this.auditLogService = auditLogService;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public TrainingSessionCreatedVO startSession(StartTrainingSessionRequest request) {
        TrainingMode mode = parseTrainingMode(request.mode());
        TrainingPlanEntity plan = requireAccessiblePlan(request.planId());
        requireNoActiveSession(plan.getOwnerUserId());
        TrainingSessionLaunchContext launchContext = resolveLaunchContext(plan, request, mode);
        List<TrainingPlanItemEntity> planItems = resolveSessionPlanItems(plan, mode, launchContext);
        if (planItems.isEmpty()) {
            throw new BusinessException(ResultCode.CONFLICT, "Training plan does not contain items for the requested mode", 409);
        }

        long sessionSeed = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        int targetVolume = resolveTargetVolume(planItems, plan.getRiskLevel(), launchContext);
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
        session.setLaunchContextJson(trainingJsonCodec.write(launchContext));
        session.setStartedAt(LocalDateTime.now());
        session.setLastSavedAt(session.getStartedAt());
        try {
            trainingSessionMapper.insert(session);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ResultCode.CONFLICT, "Training session already in progress. Resume the active session before starting a new one.", 409);
        }

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
                parseSessionStatus(session.getStatus()),
                TrainingMode.fromCode(session.getMode()),
                session.getTotalItems(),
                session.getAnsweredItems(),
                session.getCurrentItemOrder()
        );
    }

    public PageResult<TrainingHistorySummaryVO> pageHistory(TrainingSessionPageQuery query) {
        PageQuery pageQuery = query.toPageQuery();
        var wrapper = Wrappers.<TrainingSessionEntity>lambdaQuery()
                .orderByDesc(TrainingSessionEntity::getStartedAt)
                .orderByDesc(TrainingSessionEntity::getId);

        if (query.status() != null && !query.status().isBlank()) {
            wrapper.eq(TrainingSessionEntity::getStatus, parseSessionStatus(query.status()).name());
        }
        if (query.planId() != null) {
            wrapper.eq(TrainingSessionEntity::getPlanId, query.planId());
        }

        Long ownerFilter = resolveHistoryOwnerFilter(query);
        if (ownerFilter != null) {
            wrapper.eq(TrainingSessionEntity::getOwnerUserId, ownerFilter);
        }

        long total = trainingSessionMapper.selectCount(wrapper);
        List<TrainingSessionEntity> sessions = trainingSessionMapper.selectList(wrapper
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));

        List<TrainingHistorySummaryVO> records = sessions.stream()
                .map(session -> new TrainingHistorySummaryVO(
                        session.getId(),
                        session.getPlanId(),
                        session.getOwnerUserId(),
                        parseSessionStatus(session.getStatus()),
                        TrainingMode.fromCode(session.getMode()),
                        session.getTotalItems(),
                        session.getAnsweredItems(),
                        session.getCurrentItemOrder(),
                        session.getStartedAt(),
                        session.getLastSavedAt(),
                        session.getCompletedAt()
                ))
                .toList();

        return new PageResult<>(total, pageQuery.pageNo(), pageQuery.pageSize(), records);
    }

    @Transactional
    public TrainingNextItemVO getNextItem(Long sessionId) {
        TrainingSessionEntity session = requireAccessibleSession(sessionId);
        if (!TrainingSessionStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            return new TrainingNextItemVO(
                    session.getId(),
                    parseSessionStatus(session.getStatus()),
                    TrainingMode.fromCode(session.getMode()),
                    session.getTotalItems(),
                    session.getAnsweredItems(),
                    session.getCurrentItemOrder(),
                    false,
                    false,
                    parseCompletionHooksStatus(session.getCompletionHooksStatus()),
                    null
            );
        }

        TrainingItemResultEntity itemResult = trainingItemResultMapper.selectOne(Wrappers.<TrainingItemResultEntity>lambdaQuery()
                .eq(TrainingItemResultEntity::getSessionId, sessionId)
                .eq(TrainingItemResultEntity::getAnswerState, TrainingAnswerState.PENDING.name())
                .orderByAsc(TrainingItemResultEntity::getPresentationOrder)
                .last("LIMIT 1"));
        if (itemResult == null) {
            session.setCurrentItemOrder(null);
            session.setLastSavedAt(LocalDateTime.now());
            trainingSessionMapper.updateById(session);
            return new TrainingNextItemVO(
                    session.getId(),
                    parseSessionStatus(session.getStatus()),
                    TrainingMode.fromCode(session.getMode()),
                    session.getTotalItems(),
                    session.getAnsweredItems(),
                    null,
                    false,
                    isSessionReadyToComplete(session),
                    parseCompletionHooksStatus(session.getCompletionHooksStatus()),
                    null
            );
        }

        TrainingPlanItemEntity planItem = trainingPlanItemMapper.selectById(itemResult.getPlanItemId());
        LexicalPairEntity pair = lexicalPairMapper.selectById(itemResult.getLexicalPairId());
        session.setCurrentItemOrder(itemResult.getPresentationOrder());
        session.setLastSavedAt(LocalDateTime.now());
        trainingSessionMapper.updateById(session);

        return new TrainingNextItemVO(
                session.getId(),
                parseSessionStatus(session.getStatus()),
                TrainingMode.fromCode(session.getMode()),
                session.getTotalItems(),
                session.getAnsweredItems(),
                itemResult.getPresentationOrder(),
                true,
                false,
                parseCompletionHooksStatus(session.getCompletionHooksStatus()),
                toQuestionItemVO(itemResult, planItem, pair)
        );
    }

    @Transactional
    public TrainingSessionProgressVO saveProgress(Long sessionId, SaveTrainingProgressRequest request) {
        TrainingSessionEntity session = requireAccessibleSession(sessionId);
        if (TrainingSessionStatus.COMPLETED.name().equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Training session is already completed", 409);
        }
        if (!TrainingSessionStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Training session is not in progress", 409);
        }
        session.setProgressSnapshotJson(trainingJsonCodec.write(request.progressSnapshot()));
        session.setLastSavedAt(LocalDateTime.now());
        trainingSessionMapper.updateById(session);
        auditLogService.record("save_training_progress", "training_session", String.valueOf(sessionId), request, ResultCode.SUCCESS.code());
        log.info("event=training_progress_saved sessionId={} answeredItems={}/{}", sessionId, session.getAnsweredItems(), session.getTotalItems());
        return progressVO(session);
    }

    @Transactional
    public TrainingSessionHeartbeatVO heartbeatSession(Long sessionId) {
        TrainingSessionEntity session = requireAccessibleSession(sessionId);
        if (!TrainingSessionStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            return heartbeatVO(session);
        }
        LocalDateTime heartbeatAt = LocalDateTime.now();
        if (trainingSessionMapper.touchIfInProgress(sessionId, heartbeatAt) == 0) {
            TrainingSessionEntity refreshedSession = trainingSessionMapper.selectById(sessionId);
            return heartbeatVO(refreshedSession == null ? session : refreshedSession);
        }
        session.setLastSavedAt(heartbeatAt);
        return heartbeatVO(session);
    }

    @Transactional
    public TrainingSessionProgressVO submitAnswer(Long sessionId, SubmitTrainingAnswerRequest request) {
        TrainingSessionEntity accessibleSession = requireAccessibleSession(sessionId);
        IdempotencyService.IdempotencyClaimResult<TrainingSessionProgressVO> idempotencyClaim = beginAnswerIdempotency(accessibleSession, request);
        if (idempotencyClaim.replayedResponse() != null) {
            return idempotencyClaim.replayedResponse();
        }

        try {
            TrainingSessionEntity session = requireInProgressSession(sessionId);
            TrainingItemResultEntity itemResult = requireItemResult(sessionId, request.itemResultId());
            List<TrainingOptionPayload> options = trainingJsonCodec.readOptions(itemResult.getOptionsJson());
            boolean optionExists = options.stream().anyMatch(option -> option.key().equalsIgnoreCase(request.selectedAnswerKey()));
            if (!optionExists) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "Selected answer key is not defined in this training item", 400);
            }
            LexicalPairEntity pair = requireLexicalPair(itemResult.getLexicalPairId());
            boolean correct = itemResult.getCorrectAnswerKey().equalsIgnoreCase(request.selectedAnswerKey());
            String errorType = resolveErrorType(itemResult.getMode(), pair.getLexicalPairType(), correct);
            String adaptationAction = resolveAdaptationAction(itemResult.getMode(), pair.getLexicalPairType(), correct, request.reactionTimeMs(), errorType);

            int updatedRows = trainingItemResultMapper.submitAnswer(
                    itemResult.getId(),
                    sessionId,
                    TrainingAnswerState.PENDING.name(),
                    TrainingAnswerState.ANSWERED.name(),
                    request.selectedAnswerKey(),
                    trainingJsonCodec.write(Map.of(
                            "selectedAnswerKey", request.selectedAnswerKey(),
                            "reactionTimeMs", request.reactionTimeMs(),
                            "hesitationTimeMs", request.hesitationTimeMs()
                    )),
                    LocalDateTime.now(),
                    request.reactionTimeMs(),
                    request.hesitationTimeMs(),
                    correct,
                    errorType,
                    !correct,
                    adaptationAction
            );
            if (updatedRows == 0) {
                throw answeredItemConflict();
            }

            TrainingItemResultEntity nextPending = trainingItemResultMapper.selectOne(Wrappers.<TrainingItemResultEntity>lambdaQuery()
                    .eq(TrainingItemResultEntity::getSessionId, sessionId)
                    .eq(TrainingItemResultEntity::getAnswerState, TrainingAnswerState.PENDING.name())
                    .orderByAsc(TrainingItemResultEntity::getPresentationOrder)
                    .last("LIMIT 1"));
            int sessionRows = trainingSessionMapper.incrementAnsweredItems(
                    session.getId(),
                    nextPending == null ? session.getTotalItems() : nextPending.getPresentationOrder(),
                    TrainingSessionStatus.IN_PROGRESS.name()
            );
            if (sessionRows == 0) {
                throw new BusinessException(ResultCode.CONFLICT, "Training session is not in progress", 409);
            }

            session = requireAccessibleSession(session.getId());
            TrainingSessionProgressVO progress = progressVO(session);
            auditLogService.record("submit_training_answer", "training_session", String.valueOf(sessionId), request, ResultCode.SUCCESS.code());
            if (idempotencyClaim.claimed()) {
                idempotencyService.complete(idempotencyClaim.requestKey(), progress, currentUserId());
            }
            return progress;
        } catch (RuntimeException exception) {
            if (idempotencyClaim.claimed()) {
                idempotencyService.release(idempotencyClaim.requestKey());
            }
            throw exception;
        }
    }

    @Transactional
    public TrainingSessionProgressVO completeSession(Long sessionId) {
        TrainingSessionEntity session = requireAccessibleSession(sessionId);
        if (TrainingSessionStatus.COMPLETED.name().equals(session.getStatus())) {
            return progressVO(session);
        }
        if (!TrainingSessionStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Training session is not in progress", 409);
        }
        if (!isSessionReadyToComplete(session)) {
            throw new BusinessException(ResultCode.CONFLICT, "Training session still has unanswered items", 409);
        }
        session = finalizeCompletedSession(session);
        return progressVO(session);
    }

    @Transactional
    public TrainingSessionProgressVO abandonSession(Long sessionId) {
        TrainingSessionEntity session = requireAccessibleSession(sessionId);
        if (TrainingSessionStatus.ABANDONED.name().equals(session.getStatus())) {
            return progressVO(session);
        }
        if (TrainingSessionStatus.COMPLETED.name().equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Completed training session cannot be abandoned", 409);
        }
        if (!TrainingSessionStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Training session is not in progress", 409);
        }
        boolean abandoned = abandonSession(session);
        if (!abandoned) {
            session = requireAccessibleSession(sessionId);
        }
        if (TrainingSessionStatus.ABANDONED.name().equals(session.getStatus())) {
            auditLogService.record("abandon_training_session", "training_session", String.valueOf(sessionId), Map.of("sessionId", sessionId), ResultCode.SUCCESS.code());
        }
        return progressVO(session);
    }

    public TrainingSessionSummaryVO getSummary(Long sessionId) {
        TrainingSessionEntity session = requireAccessibleSession(sessionId);
        if (!TrainingSessionStatus.COMPLETED.name().equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Training session is not completed", 409);
        }
        TrainingSessionSummarySnapshot summarySnapshot = trainingJsonCodec.readSummarySnapshot(session.getSummarySnapshotJson());
        List<TrainingItemResultEntity> itemResults = listSessionItems(session.getId());
        Map<Long, TrainingPlanItemEntity> planItemMap = loadPlanItemMap(itemResults.stream()
                .map(TrainingItemResultEntity::getPlanItemId)
                .filter(Objects::nonNull)
                .toList());
        Map<Long, LexicalPairEntity> pairMap = loadLexicalPairMap(itemResults.stream().map(TrainingItemResultEntity::getLexicalPairId).toList());
        return new TrainingSessionSummaryVO(
                session.getId(),
                TrainingMode.fromCode(session.getMode()),
                summarySnapshot.accuracy(),
                summarySnapshot.averageReactionTime(),
                summarySnapshot.improvementHint(),
                TrainingMode.fromCode(summarySnapshot.nextRecommendedMode()),
                summarySnapshot.riskWordsToReview().stream()
                        .map(this::toRiskWordVO)
                        .toList(),
                itemResults.stream()
                        .map(itemResult -> toItemResultDetailVO(
                                itemResult,
                                planItemMap.get(itemResult.getPlanItemId()),
                                pairMap.get(itemResult.getLexicalPairId())
                        ))
                        .toList()
        );
    }

    private TrainingSessionEntity finalizeCompletedSession(TrainingSessionEntity session) {
        if (TrainingSessionStatus.COMPLETED.name().equals(session.getStatus())) {
            return session;
        }
        if (!isSessionReadyToComplete(session)) {
            throw new BusinessException(ResultCode.CONFLICT, "Training session still has unanswered items", 409);
        }

        List<TrainingItemResultEntity> itemResults = listSessionItems(session.getId());
        Map<Long, LexicalPairEntity> pairMap = loadLexicalPairMap(itemResults.stream().map(TrainingItemResultEntity::getLexicalPairId).toList());
        TrainingSessionSummarySnapshot summarySnapshot = buildSummarySnapshot(session, itemResults, pairMap);
        session.setStatus(TrainingSessionStatus.COMPLETED.name());
        session.setCompletedAt(LocalDateTime.now());
        session.setCurrentItemOrder(null);
        session.setLastSavedAt(session.getCompletedAt());
        session.setCompletionHooksStatus(SessionCompletionHookStatus.PENDING.name());
        session.setCompletionHooksUpdatedAt(session.getCompletedAt());
        session.setCompletionHooksError(null);
        session.setSummarySnapshotJson(trainingJsonCodec.write(summarySnapshot));
        trainingSessionMapper.updateById(session);
        trainingSessionCompletionService.triggerAfterCommit(session.getId());

        auditLogService.record("complete_training_session", "training_session", String.valueOf(session.getId()), Map.of("sessionId", session.getId()), ResultCode.SUCCESS.code());
        log.info("event=training_session_completed sessionId={} planId={} ownerUserId={} accuracy={} averageReactionTime={}",
                session.getId(), session.getPlanId(), session.getOwnerUserId(), summarySnapshot.accuracy(), summarySnapshot.averageReactionTime());
        return session;
    }

    private boolean isSessionReadyToComplete(TrainingSessionEntity session) {
        return isSessionReadyToComplete(session, countAnsweredItems(session.getId()));
    }

    private boolean isSessionReadyToComplete(TrainingSessionEntity session, int answeredCount) {
        return countPendingItems(session.getId()) == 0 && answeredCount >= session.getTotalItems();
    }

    private int countAnsweredItems(Long sessionId) {
        return Math.toIntExact(trainingItemResultMapper.selectCount(Wrappers.<TrainingItemResultEntity>lambdaQuery()
                .eq(TrainingItemResultEntity::getSessionId, sessionId)
                .eq(TrainingItemResultEntity::getAnswerState, TrainingAnswerState.ANSWERED.name())));
    }

    private long countPendingItems(Long sessionId) {
        return trainingItemResultMapper.selectCount(Wrappers.<TrainingItemResultEntity>lambdaQuery()
                .eq(TrainingItemResultEntity::getSessionId, sessionId)
                .eq(TrainingItemResultEntity::getAnswerState, TrainingAnswerState.PENDING.name()));
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
        List<TrainingRiskWordSnapshot> riskWordsToReview = summarizeRiskWordsFromSession(itemResults, pairMap);
        return new TrainingSessionSummarySnapshot(accuracy, averageReactionTime, improvementHint, nextRecommendedMode, riskWordsToReview);
    }

    private List<TrainingRiskWordSnapshot> summarizeRiskWordsFromSession(
            List<TrainingItemResultEntity> itemResults,
            Map<Long, LexicalPairEntity> pairMap
    ) {
        return itemResults.stream()
                .collect(Collectors.groupingBy(TrainingItemResultEntity::getLexicalPairId, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    LexicalPairEntity pair = pairMap.get(entry.getKey());
                    if (pair == null) {
                        return null;
                    }
                    List<TrainingItemResultEntity> results = entry.getValue();
                    long wrongCount = results.stream().filter(result -> Boolean.FALSE.equals(result.getIsCorrect())).count();
                    long slowCount = results.stream()
                            .filter(result -> result.getReactionTimeMs() != null && result.getReactionTimeMs() >= SLOW_REACTION_THRESHOLD_MS)
                            .count();
                    double avgReactionTime = results.stream()
                            .filter(result -> result.getReactionTimeMs() != null)
                            .mapToInt(TrainingItemResultEntity::getReactionTimeMs)
                            .average()
                            .orElse(0);
                    if (wrongCount == 0 && slowCount == 0) {
                        return null;
                    }
                    String dominantErrorType = results.stream()
                            .map(TrainingItemResultEntity::getDetectedErrorType)
                            .filter(Objects::nonNull)
                            .collect(Collectors.groupingBy(errorType -> errorType, LinkedHashMap::new, Collectors.counting()))
                            .entrySet()
                            .stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(null);
                    String reason = wrongCount > 0
                            ? "本轮训练中出现 " + wrongCount + " 次错误"
                            : "本轮反应偏慢，建议继续压缩识别时间";
                    return new TrainingRiskWordSnapshot(
                            pair.getId(),
                            pair.getEnglishWord(),
                            pair.getFrenchWord(),
                            pair.getChineseGloss(),
                            pair.getLexicalPairType(),
                            reason,
                            resolveRiskLevel((int) wrongCount).name(),
                            dominantErrorType
                    );
                })
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing((TrainingRiskWordSnapshot snapshot) -> RiskLevel.fromCode(snapshot.riskLevel())).reversed()
                        .thenComparing(TrainingRiskWordSnapshot::dominantErrorType, Comparator.nullsLast(String::compareTo))
                        .thenComparing(TrainingRiskWordSnapshot::englishWord, Comparator.nullsLast(String::compareTo)))
                .limit(5)
                .toList();
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

    private void advanceReviewIfApplicable(
            TrainingSessionEntity session,
            LexicalPairEntity pair,
            TrainingSessionLaunchContext launchContext
    ) {
        WrongBookEntity wrongBook = wrongBookMapper.selectOne(Wrappers.<WrongBookEntity>lambdaQuery()
                .eq(WrongBookEntity::getOwnerUserId, session.getOwnerUserId())
                .eq(WrongBookEntity::getLexicalPairId, pair.getId()));
        if (wrongBook == null) {
            return;
        }
        ReviewScheduleEntity pending = null;
        if (launchContext != null
                && launchContext.targeted()
                && Objects.equals(launchContext.lexicalPairId(), pair.getId())
                && launchContext.reviewScheduleId() != null) {
            ReviewScheduleEntity targetedSchedule = reviewScheduleMapper.selectById(launchContext.reviewScheduleId());
            if (targetedSchedule != null
                    && Objects.equals(targetedSchedule.getOwnerUserId(), session.getOwnerUserId())
                    && ReviewScheduleStatus.PENDING.name().equals(targetedSchedule.getStatus())) {
                pending = targetedSchedule;
            }
        }
        if (pending == null) {
            pending = reviewScheduleMapper.selectOne(Wrappers.<ReviewScheduleEntity>lambdaQuery()
                    .eq(ReviewScheduleEntity::getOwnerUserId, session.getOwnerUserId())
                    .eq(ReviewScheduleEntity::getLexicalPairId, pair.getId())
                    .eq(ReviewScheduleEntity::getStatus, ReviewScheduleStatus.PENDING.name())
                    .orderByAsc(ReviewScheduleEntity::getDueAt)
                    .orderByAsc(ReviewScheduleEntity::getId)
                    .last("LIMIT 1"));
        }
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
                LexicalPairType.fromCode(snapshot.lexicalPairType()),
                snapshot.reason(),
                snapshot.riskLevel(),
                snapshot.dominantErrorType()
        );
    }

    private TrainingQuestionItemVO toQuestionItemVO(
            TrainingItemResultEntity itemResult,
            TrainingPlanItemEntity planItem,
            LexicalPairEntity pair
    ) {
        TrainingStimulusPayload stimulus = trainingJsonCodec.readStimulus(itemResult.getStimulusJson());
        List<TrainingOptionViewVO> options = trainingJsonCodec.readOptions(itemResult.getOptionsJson()).stream()
                .map(option -> new TrainingOptionViewVO(option.key(), option.label()))
                .toList();
        return new TrainingQuestionItemVO(
                itemResult.getId(),
                itemResult.getPlanItemId(),
                TrainingMode.fromCode(itemResult.getMode()),
                TrainingItemType.fromCode(itemResult.getItemType()),
                itemResult.getPresentationOrder(),
                itemResult.getLexicalPairId(),
                pair == null ? null : pair.getEnglishWord(),
                pair == null ? null : pair.getFrenchWord(),
                pair == null ? null : pair.getChineseGloss(),
                pair == null ? null : LexicalPairType.fromCode(pair.getLexicalPairType()),
                pair == null ? null : new TrainingWordPairVO(
                        pair.getEnglishWord(),
                        pair.getFrenchWord(),
                        pair.getChineseGloss(),
                        frontendPairType(pair.getLexicalPairType())
                ),
                planItem == null ? null : planItem.getRecommendedDifficulty(),
                TrainingCognitiveTag.fromCode(itemResult.getCognitiveTag()),
                new TrainingExerciseContentVO(
                        stimulus.questionText(),
                        options.stream().map(TrainingOptionViewVO::label).toList(),
                        stimulus.explanation(),
                        stimulus.contextSupportLevel(),
                        stimulus.contextSentence()
                ),
                stimulus,
                options
        );
    }

    private TrainingItemResultDetailVO toItemResultDetailVO(
            TrainingItemResultEntity itemResult,
            TrainingPlanItemEntity planItem,
            LexicalPairEntity pair
    ) {
        TrainingQuestionItemVO questionItem = toQuestionItemVO(itemResult, planItem, pair);
        return new TrainingItemResultDetailVO(
                questionItem.itemResultId(),
                questionItem.planItemId(),
                questionItem.presentationOrder(),
                questionItem.mode(),
                questionItem.itemType(),
                questionItem.lexicalPairId(),
                questionItem.englishWord(),
                questionItem.frenchWord(),
                questionItem.chineseGloss(),
                questionItem.lexicalPairType(),
                questionItem.wordPair(),
                questionItem.difficultyLevel(),
                questionItem.cognitiveTag(),
                questionItem.content(),
                questionItem.stimulus(),
                questionItem.options(),
                itemResult.getCorrectAnswerKey(),
                itemResult.getSelectedAnswerKey(),
                itemResult.getSubmittedAt(),
                itemResult.getReactionTimeMs(),
                itemResult.getHesitationTimeMs(),
                itemResult.getIsCorrect(),
                itemResult.getDetectedErrorType(),
                itemResult.getReviewRequired(),
                itemResult.getAdaptationAction()
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

    private void requireNoActiveSession(Long ownerUserId) {
        if (trainingSessionMapper.selectCount(Wrappers.<TrainingSessionEntity>lambdaQuery()
                .eq(TrainingSessionEntity::getOwnerUserId, ownerUserId)
                .eq(TrainingSessionEntity::getStatus, TrainingSessionStatus.IN_PROGRESS.name())) > 0) {
            throw new BusinessException(ResultCode.CONFLICT, "Training session already in progress. Resume the active session before starting a new one.", 409);
        }
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

    private TrainingItemResultEntity requireItemResult(Long sessionId, Long itemResultId) {
        TrainingItemResultEntity itemResult = trainingItemResultMapper.selectOne(Wrappers.<TrainingItemResultEntity>lambdaQuery()
                .eq(TrainingItemResultEntity::getId, itemResultId)
                .eq(TrainingItemResultEntity::getSessionId, sessionId));
        if (itemResult == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Training item result was not found", 404);
        }
        return itemResult;
    }

    private TrainingSessionLaunchContext resolveLaunchContext(
            TrainingPlanEntity plan,
            StartTrainingSessionRequest request,
            TrainingMode mode
    ) {
        if (request.diagnosisSummaryId() != null
                && !Objects.equals(plan.getSourceDiagnosisSummaryId(), request.diagnosisSummaryId())) {
            throw new BusinessException(ResultCode.CONFLICT, "Training plan does not match the requested diagnosis summary", 409);
        }

        Long lexicalPairId = request.lexicalPairId();
        if (request.wrongBookId() != null) {
            WrongBookEntity wrongBook = requireAccessibleWrongBook(request.wrongBookId(), plan.getOwnerUserId());
            lexicalPairId = mergeTargetLexicalPair(lexicalPairId, wrongBook.getLexicalPairId(), "wrongBook");
        }
        if (request.reviewScheduleId() != null) {
            ReviewScheduleEntity reviewSchedule = requireAccessibleReviewSchedule(request.reviewScheduleId(), plan.getOwnerUserId());
            if (!ReviewScheduleStatus.PENDING.name().equals(reviewSchedule.getStatus())) {
                throw new BusinessException(ResultCode.CONFLICT, "Review schedule is no longer pending", 409);
            }
            if (!mode.name().equalsIgnoreCase(reviewSchedule.getReviewMode())) {
                throw new BusinessException(ResultCode.CONFLICT, "Review schedule must be launched with its configured mode", 409);
            }
            lexicalPairId = mergeTargetLexicalPair(lexicalPairId, reviewSchedule.getLexicalPairId(), "reviewSchedule");
            if (request.wrongBookId() != null && !Objects.equals(reviewSchedule.getWrongBookId(), request.wrongBookId())) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, "reviewScheduleId does not belong to the provided wrongBookId", 400);
            }
        }

        return new TrainingSessionLaunchContext(
                blankToNull(request.launchSource()),
                request.diagnosisSummaryId() == null ? plan.getSourceDiagnosisSummaryId() : request.diagnosisSummaryId(),
                lexicalPairId,
                request.wrongBookId(),
                request.reviewScheduleId()
        );
    }

    private Long mergeTargetLexicalPair(Long requestedLexicalPairId, Long sourceLexicalPairId, String sourceName) {
        if (requestedLexicalPairId == null) {
            return sourceLexicalPairId;
        }
        if (!Objects.equals(requestedLexicalPairId, sourceLexicalPairId)) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, sourceName + " does not match the provided lexicalPairId", 400);
        }
        return requestedLexicalPairId;
    }

    private WrongBookEntity requireAccessibleWrongBook(Long wrongBookId, Long ownerUserId) {
        WrongBookEntity wrongBook = wrongBookMapper.selectById(wrongBookId);
        if (wrongBook == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Wrong book item was not found", 404);
        }
        if (!Objects.equals(wrongBook.getOwnerUserId(), ownerUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have permission to access this wrong book item", 403);
        }
        return wrongBook;
    }

    private ReviewScheduleEntity requireAccessibleReviewSchedule(Long reviewScheduleId, Long ownerUserId) {
        ReviewScheduleEntity reviewSchedule = reviewScheduleMapper.selectById(reviewScheduleId);
        if (reviewSchedule == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Review schedule was not found", 404);
        }
        if (!Objects.equals(reviewSchedule.getOwnerUserId(), ownerUserId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have permission to access this review schedule", 403);
        }
        return reviewSchedule;
    }

    private List<TrainingPlanItemEntity> resolveSessionPlanItems(
            TrainingPlanEntity plan,
            TrainingMode mode,
            TrainingSessionLaunchContext launchContext
    ) {
        List<TrainingPlanItemEntity> planItems = loadPlanItems(plan.getId(), mode);
        if (launchContext == null || !launchContext.targeted()) {
            return planItems;
        }
        List<TrainingPlanItemEntity> targetedItems = planItems.stream()
                .filter(item -> Objects.equals(item.getLexicalPairId(), launchContext.lexicalPairId()))
                .toList();
        if (!targetedItems.isEmpty()) {
            return targetedItems;
        }
        return List.of(ensureTargetPlanItem(plan, mode, launchContext));
    }

    private TrainingPlanItemEntity ensureTargetPlanItem(
            TrainingPlanEntity plan,
            TrainingMode mode,
            TrainingSessionLaunchContext launchContext
    ) {
        TrainingPlanItemEntity existingModeItem = trainingPlanItemMapper.selectOne(Wrappers.<TrainingPlanItemEntity>lambdaQuery()
                .eq(TrainingPlanItemEntity::getPlanId, plan.getId())
                .eq(TrainingPlanItemEntity::getLexicalPairId, launchContext.lexicalPairId())
                .eq(TrainingPlanItemEntity::getRecommendedMode, mode.name())
                .orderByDesc(TrainingPlanItemEntity::getPriorityScore)
                .orderByAsc(TrainingPlanItemEntity::getSortOrder)
                .orderByAsc(TrainingPlanItemEntity::getId)
                .last("LIMIT 1"));
        if (existingModeItem != null) {
            return existingModeItem;
        }

        TrainingPlanItemEntity referenceItem = trainingPlanItemMapper.selectOne(Wrappers.<TrainingPlanItemEntity>lambdaQuery()
                .eq(TrainingPlanItemEntity::getPlanId, plan.getId())
                .eq(TrainingPlanItemEntity::getLexicalPairId, launchContext.lexicalPairId())
                .orderByDesc(TrainingPlanItemEntity::getPriorityScore)
                .orderByAsc(TrainingPlanItemEntity::getSortOrder)
                .orderByAsc(TrainingPlanItemEntity::getId)
                .last("LIMIT 1"));
        LexicalPairEntity pair = requireLexicalPair(launchContext.lexicalPairId());

        TrainingPlanItemEntity syntheticItem = new TrainingPlanItemEntity();
        syntheticItem.setPlanId(plan.getId());
        syntheticItem.setLexicalPairId(launchContext.lexicalPairId());
        syntheticItem.setRecommendedMode(mode.name());
        syntheticItem.setRecommendedDifficulty(referenceItem == null ? plan.getRecommendedDifficulty() : referenceItem.getRecommendedDifficulty());
        syntheticItem.setRiskLevel(referenceItem == null ? plan.getRiskLevel() : referenceItem.getRiskLevel());
        syntheticItem.setPriorityScore(referenceItem == null ? BigDecimal.ONE : referenceItem.getPriorityScore());
        syntheticItem.setRecommendedReason(referenceItem == null
                ? "定向训练入口补充：" + pair.getEnglishWord() + " / " + pair.getFrenchWord()
                : referenceItem.getRecommendedReason());
        syntheticItem.setDominantErrorType(referenceItem == null ? null : referenceItem.getDominantErrorType());
        syntheticItem.setTargetContextSupport(referenceItem == null ? pair.getDefaultContextSupport() : referenceItem.getTargetContextSupport());
        syntheticItem.setExpectedExposures(referenceItem == null || referenceItem.getExpectedExposures() == null
                ? 3
                : Math.max(3, referenceItem.getExpectedExposures()));
        syntheticItem.setSortOrder(nextSortOrder(plan.getId()));
        trainingPlanItemMapper.insert(syntheticItem);
        return syntheticItem;
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

    private int nextSortOrder(Long planId) {
        return trainingPlanItemMapper.selectList(Wrappers.<TrainingPlanItemEntity>lambdaQuery()
                        .eq(TrainingPlanItemEntity::getPlanId, planId))
                .stream()
                .map(TrainingPlanItemEntity::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private int resolveTargetVolume(
            List<TrainingPlanItemEntity> planItems,
            String riskLevel,
            TrainingSessionLaunchContext launchContext
    ) {
        if (launchContext != null && launchContext.targeted()) {
            return Math.max(3, Math.min(5, planItems.stream()
                    .map(TrainingPlanItemEntity::getExpectedExposures)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElse(3)));
        }
        return targetVolume(riskLevel);
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

    private Map<Long, TrainingPlanItemEntity> loadPlanItemMap(Collection<Long> planItemIds) {
        if (planItemIds.isEmpty()) {
            return Map.of();
        }
        return trainingPlanItemMapper.selectBatchIds(new LinkedHashSet<>(planItemIds))
                .stream()
                .collect(Collectors.toMap(TrainingPlanItemEntity::getId, Function.identity()));
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

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private TrainingSessionProgressVO progressVO(TrainingSessionEntity session) {
        return new TrainingSessionProgressVO(
                session.getId(),
                parseSessionStatus(session.getStatus()),
                session.getTotalItems(),
                session.getAnsweredItems(),
                session.getCurrentItemOrder(),
                TrainingSessionStatus.COMPLETED.name().equals(session.getStatus()),
                TrainingSessionStatus.IN_PROGRESS.name().equals(session.getStatus()) && isSessionReadyToComplete(session),
                parseCompletionHooksStatus(session.getCompletionHooksStatus())
        );
    }

    private TrainingSessionHeartbeatVO heartbeatVO(TrainingSessionEntity session) {
        return new TrainingSessionHeartbeatVO(
                session.getId(),
                parseSessionStatus(session.getStatus()),
                session.getAnsweredItems(),
                session.getCurrentItemOrder(),
                session.getLastSavedAt()
        );
    }

    private Long resolveHistoryOwnerFilter(TrainingSessionPageQuery query) {
        if (isAdmin()) {
            if (query.ownerUserId() != null) {
                return query.ownerUserId();
            }
            return Boolean.TRUE.equals(query.mineOnly()) ? currentUserId() : null;
        }
        return currentUserId();
    }

    @Transactional
    public int completeTimedOutReadySessions(LocalDateTime cutoff, int limit) {
        if (limit <= 0) {
            return 0;
        }
        int completed = 0;
        for (Long sessionId : trainingSessionMapper.selectTimedOutReadySessionIds(cutoff, limit)) {
            if (completeTimedOutReadySession(sessionId, cutoff)) {
                completed++;
            }
        }
        return completed;
    }

    @Transactional
    public int abandonTimedOutSessions(LocalDateTime cutoff, int limit) {
        if (limit <= 0) {
            return 0;
        }
        return trainingSessionMapper.batchAbandonTimedOutSessions(cutoff, LocalDateTime.now(), limit);
    }

    private boolean abandonSession(TrainingSessionEntity session) {
        LocalDateTime abandonedAt = LocalDateTime.now();
        if (trainingSessionMapper.abandonIfInProgress(session.getId(), abandonedAt) == 0) {
            return false;
        }
        session.setStatus(TrainingSessionStatus.ABANDONED.name());
        session.setCurrentItemOrder(null);
        session.setLastSavedAt(abandonedAt);
        return true;
    }

    private boolean completeTimedOutReadySession(Long sessionId, LocalDateTime cutoff) {
        TrainingSessionEntity session = trainingSessionMapper.selectByIdForUpdate(sessionId);
        if (session == null || !TrainingSessionStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            return false;
        }
        if (session.getLastSavedAt() == null || !session.getLastSavedAt().isBefore(cutoff)) {
            return false;
        }
        if (!isSessionReadyToComplete(session)) {
            return false;
        }
        finalizeCompletedSession(session);
        return true;
    }

    private IdempotencyService.IdempotencyClaimResult<TrainingSessionProgressVO> beginAnswerIdempotency(
            TrainingSessionEntity session,
            SubmitTrainingAnswerRequest request
    ) {
        if (!hasText(request.clientRequestId())) {
            return new IdempotencyService.IdempotencyClaimResult<>(null, false, null);
        }
        return idempotencyService.claimOrReplay(
                buildAnswerRequestKey("training", session.getId(), request.itemResultId(), request.clientRequestId()),
                idempotencyService.hashPayload(request),
                currentUserId(),
                TrainingSessionProgressVO.class
        );
    }

    private String buildAnswerRequestKey(String domain, Long sessionId, Long itemResultId, String clientRequestId) {
        String rawKey = sessionId + ":" + itemResultId + ":" + clientRequestId.trim();
        return domain + ":answer:" + TokenGenerator.sha256(rawKey);
    }

    private BusinessException answeredItemConflict() {
        return new BusinessException(ResultCode.CONFLICT, "Training item has already been answered", 409);
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

    private TrainingSessionStatus parseSessionStatus(String value) {
        try {
            return TrainingSessionStatus.fromCode(value.trim());
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported trainingSessionStatus: " + value, 400);
        }
    }

    private SessionCompletionHookStatus parseCompletionHooksStatus(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return SessionCompletionHookStatus.valueOf(value.trim());
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported session completion hook status: " + value, 400);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record SessionPlanItem(
            TrainingPlanItemEntity planItem,
            int exposureIndex
    ) {
    }
}
