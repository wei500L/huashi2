package com.huashi.eftransfer.app.modules.training.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.session.SessionCompletionHookStatus;
import com.huashi.eftransfer.app.common.session.SessionLifecycleProperties;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairMapper;
import com.huashi.eftransfer.app.modules.training.entity.ReviewScheduleEntity;
import com.huashi.eftransfer.app.modules.training.entity.TrainingItemResultEntity;
import com.huashi.eftransfer.app.modules.training.entity.TrainingPlanEntity;
import com.huashi.eftransfer.app.modules.training.entity.TrainingSessionEntity;
import com.huashi.eftransfer.app.modules.training.entity.WrongBookEntity;
import com.huashi.eftransfer.app.modules.training.event.TrainingCompletedEvent;
import com.huashi.eftransfer.app.modules.training.event.TrainingCompletedEventPublisher;
import com.huashi.eftransfer.app.modules.training.mapper.ReviewScheduleMapper;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingItemResultMapper;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingPlanMapper;
import com.huashi.eftransfer.app.modules.training.mapper.TrainingSessionMapper;
import com.huashi.eftransfer.app.modules.training.mapper.WrongBookMapper;
import com.huashi.eftransfer.app.modules.training.support.TrainingJsonCodec;
import com.huashi.eftransfer.app.modules.training.support.TrainingLearningProfileSnapshot;
import com.huashi.eftransfer.app.modules.training.support.TrainingRiskWordSnapshot;
import com.huashi.eftransfer.app.modules.training.support.TrainingSessionLaunchContext;
import com.huashi.eftransfer.app.modules.training.support.TrainingSessionSummarySnapshot;
import com.huashi.eftransfer.app.modules.user.entity.StudentProfileEntity;
import com.huashi.eftransfer.app.modules.user.mapper.StudentProfileMapper;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.LexicalPairType;
import com.huashi.eftransfer.shared.enums.ReviewScheduleStatus;
import com.huashi.eftransfer.shared.enums.RiskLevel;
import com.huashi.eftransfer.shared.enums.TrainingMode;
import com.huashi.eftransfer.shared.enums.TrainingPlanStatus;
import com.huashi.eftransfer.shared.enums.TrainingSessionStatus;
import com.huashi.eftransfer.shared.enums.WrongBookMasteryStatus;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TrainingSessionCompletionService {

    private static final Logger log = LoggerFactory.getLogger(TrainingSessionCompletionService.class);
    private static final List<Integer> REVIEW_INTERVAL_DAYS = List.of(1, 3, 7, 14);

    private final TrainingSessionMapper trainingSessionMapper;
    private final TrainingItemResultMapper trainingItemResultMapper;
    private final WrongBookMapper wrongBookMapper;
    private final ReviewScheduleMapper reviewScheduleMapper;
    private final TrainingPlanMapper trainingPlanMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final LexicalPairMapper lexicalPairMapper;
    private final TrainingJsonCodec trainingJsonCodec;
    private final TrainingCompletedEventPublisher trainingCompletedEventPublisher;
    private final SessionLifecycleProperties sessionLifecycleProperties;
    private final TransactionTemplate transactionTemplate;
    private final TaskExecutor sessionCompletionTaskExecutor;

    public TrainingSessionCompletionService(
            TrainingSessionMapper trainingSessionMapper,
            TrainingItemResultMapper trainingItemResultMapper,
            WrongBookMapper wrongBookMapper,
            ReviewScheduleMapper reviewScheduleMapper,
            TrainingPlanMapper trainingPlanMapper,
            StudentProfileMapper studentProfileMapper,
            LexicalPairMapper lexicalPairMapper,
            TrainingJsonCodec trainingJsonCodec,
            TrainingCompletedEventPublisher trainingCompletedEventPublisher,
            SessionLifecycleProperties sessionLifecycleProperties,
            TransactionTemplate transactionTemplate,
            @Qualifier("sessionCompletionTaskExecutor")
            TaskExecutor sessionCompletionTaskExecutor
    ) {
        this.trainingSessionMapper = trainingSessionMapper;
        this.trainingItemResultMapper = trainingItemResultMapper;
        this.wrongBookMapper = wrongBookMapper;
        this.reviewScheduleMapper = reviewScheduleMapper;
        this.trainingPlanMapper = trainingPlanMapper;
        this.studentProfileMapper = studentProfileMapper;
        this.lexicalPairMapper = lexicalPairMapper;
        this.trainingJsonCodec = trainingJsonCodec;
        this.trainingCompletedEventPublisher = trainingCompletedEventPublisher;
        this.sessionLifecycleProperties = sessionLifecycleProperties;
        this.transactionTemplate = transactionTemplate;
        this.sessionCompletionTaskExecutor = sessionCompletionTaskExecutor;
    }

    public void triggerAfterCommit(Long sessionId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            scheduleProcessing(sessionId);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                scheduleProcessing(sessionId);
            }
        });
    }

    public void retryPendingCompletions(int limit) {
        LocalDateTime staleBefore = LocalDateTime.now().minus(sessionLifecycleProperties.getPollInterval());
        List<Long> sessionIds = trainingSessionMapper.selectList(Wrappers.<TrainingSessionEntity>lambdaQuery()
                        .select(TrainingSessionEntity::getId)
                        .eq(TrainingSessionEntity::getStatus, TrainingSessionStatus.COMPLETED.name())
                        .and(wrapper -> wrapper
                                .in(TrainingSessionEntity::getCompletionHooksStatus,
                                        SessionCompletionHookStatus.PENDING.name(),
                                        SessionCompletionHookStatus.FAILED.name())
                                .or(stale -> stale
                                        .eq(TrainingSessionEntity::getCompletionHooksStatus, SessionCompletionHookStatus.IN_PROGRESS.name())
                                        .le(TrainingSessionEntity::getCompletionHooksUpdatedAt, staleBefore)))
                        .orderByAsc(TrainingSessionEntity::getCompletionHooksUpdatedAt)
                        .orderByAsc(TrainingSessionEntity::getId)
                        .last("LIMIT " + limit))
                .stream()
                .map(TrainingSessionEntity::getId)
                .toList();
        sessionIds.forEach(this::scheduleProcessing);
    }

    private void scheduleProcessing(Long sessionId) {
        sessionCompletionTaskExecutor.execute(() -> processSafely(sessionId, LocalDateTime.now()));
    }

    private void processSafely(Long sessionId, LocalDateTime staleReference) {
        if (trainingSessionMapper.claimCompletionHooks(sessionId, staleReference.minus(sessionLifecycleProperties.getPollInterval())) == 0) {
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> processClaimedSession(sessionId));
        } catch (RuntimeException exception) {
            markFailed(sessionId, exception);
            log.warn("event=training_completion_hooks_failed sessionId={} message={}", sessionId, exception.getMessage(), exception);
        }
    }

    private void processClaimedSession(Long sessionId) {
        TrainingSessionEntity session = trainingSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Training session was not found", 404);
        }

        List<TrainingItemResultEntity> itemResults = trainingItemResultMapper.selectList(Wrappers.<TrainingItemResultEntity>lambdaQuery()
                .eq(TrainingItemResultEntity::getSessionId, sessionId)
                .orderByAsc(TrainingItemResultEntity::getPresentationOrder)
                .orderByAsc(TrainingItemResultEntity::getId));
        Map<Long, LexicalPairEntity> pairMap = loadLexicalPairMap(itemResults.stream().map(TrainingItemResultEntity::getLexicalPairId).toList());
        Map<Long, List<TrainingItemResultEntity>> pairResultMap = itemResults.stream()
                .collect(Collectors.groupingBy(TrainingItemResultEntity::getLexicalPairId, LinkedHashMap::new, Collectors.toList()));
        TrainingSessionLaunchContext launchContext = trainingJsonCodec.readLaunchContext(session.getLaunchContextJson());

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
                advanceReviewIfApplicable(session, pair, launchContext);
            }
        }

        TrainingPlanEntity plan = trainingPlanMapper.selectById(session.getPlanId());
        TrainingSessionSummarySnapshot summarySnapshot = trainingJsonCodec.readSummarySnapshot(session.getSummarySnapshotJson());
        if (plan != null) {
            plan.setStatus(TrainingPlanStatus.COMPLETED.name());
            if (plan.getCompletedAt() == null) {
                plan.setCompletedAt(session.getCompletedAt());
            }
            trainingPlanMapper.updateById(plan);
            updateLearningProfile(plan, session, summarySnapshot);
        }

        long pendingReviewCount = reviewScheduleMapper.selectCount(Wrappers.<ReviewScheduleEntity>lambdaQuery()
                .eq(ReviewScheduleEntity::getOwnerUserId, session.getOwnerUserId())
                .eq(ReviewScheduleEntity::getStatus, ReviewScheduleStatus.PENDING.name()));
        trainingCompletedEventPublisher.publish(new TrainingCompletedEvent(
                session.getId(),
                session.getPlanId(),
                session.getOwnerUserId(),
                session.getMode(),
                session.getCompletedAt(),
                summarySnapshot.accuracy(),
                summarySnapshot.averageReactionTime(),
                summarySnapshot.nextRecommendedMode(),
                Math.toIntExact(pendingReviewCount),
                null,
                1
        ));

        session.setCompletionHooksStatus(SessionCompletionHookStatus.DONE.name());
        session.setCompletionHooksUpdatedAt(LocalDateTime.now());
        session.setCompletionHooksError(null);
        trainingSessionMapper.updateById(session);
        log.info("event=training_completion_hooks_done sessionId={} planId={}", sessionId, session.getPlanId());
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
            weaknesses.add("stable_transfer");
        }
        return weaknesses;
    }

    private Map<Long, LexicalPairEntity> loadLexicalPairMap(List<Long> lexicalPairIds) {
        if (lexicalPairIds.isEmpty()) {
            return Map.of();
        }
        return lexicalPairMapper.selectBatchIds(new LinkedHashSet<>(lexicalPairIds))
                .stream()
                .collect(Collectors.toMap(LexicalPairEntity::getId, pair -> pair));
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

    private boolean isFalseFriendLike(String lexicalPairType) {
        if (lexicalPairType == null) {
            return false;
        }
        LexicalPairType pairType = LexicalPairType.fromCode(lexicalPairType);
        return pairType == LexicalPairType.FALSE_FRIEND || pairType == LexicalPairType.ORTHOGRAPHIC_SIMILAR;
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

    private void markFailed(Long sessionId, RuntimeException exception) {
        TrainingSessionEntity session = trainingSessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }
        session.setCompletionHooksStatus(SessionCompletionHookStatus.FAILED.name());
        session.setCompletionHooksUpdatedAt(LocalDateTime.now());
        session.setCompletionHooksError(truncateError(exception.getMessage()));
        trainingSessionMapper.updateById(session);
    }

    private String truncateError(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown training completion hook failure";
        }
        return message.length() <= 512 ? message : message.substring(0, 512);
    }
}
