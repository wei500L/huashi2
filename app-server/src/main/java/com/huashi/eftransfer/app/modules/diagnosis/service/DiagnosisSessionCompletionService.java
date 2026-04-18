package com.huashi.eftransfer.app.modules.diagnosis.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.session.SessionCompletionHookStatus;
import com.huashi.eftransfer.app.common.session.SessionLifecycleProperties;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisSessionEntity;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisSummaryEntity;
import com.huashi.eftransfer.app.modules.diagnosis.event.DiagnosisCompletedEvent;
import com.huashi.eftransfer.app.modules.diagnosis.event.DiagnosisCompletedEventPublisher;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisSessionMapper;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisSummaryMapper;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisJsonCodec;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.DiagnosisSessionStatus;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DiagnosisSessionCompletionService {

    private static final Logger log = LoggerFactory.getLogger(DiagnosisSessionCompletionService.class);

    private final DiagnosisSessionMapper diagnosisSessionMapper;
    private final DiagnosisSummaryMapper diagnosisSummaryMapper;
    private final DiagnosisJsonCodec diagnosisJsonCodec;
    private final DiagnosisCompletedEventPublisher diagnosisCompletedEventPublisher;
    private final SessionLifecycleProperties sessionLifecycleProperties;
    private final TransactionTemplate transactionTemplate;
    private final TaskExecutor sessionCompletionTaskExecutor;

    public DiagnosisSessionCompletionService(
            DiagnosisSessionMapper diagnosisSessionMapper,
            DiagnosisSummaryMapper diagnosisSummaryMapper,
            DiagnosisJsonCodec diagnosisJsonCodec,
            DiagnosisCompletedEventPublisher diagnosisCompletedEventPublisher,
            SessionLifecycleProperties sessionLifecycleProperties,
            TransactionTemplate transactionTemplate,
            @Qualifier("sessionCompletionTaskExecutor")
            TaskExecutor sessionCompletionTaskExecutor
    ) {
        this.diagnosisSessionMapper = diagnosisSessionMapper;
        this.diagnosisSummaryMapper = diagnosisSummaryMapper;
        this.diagnosisJsonCodec = diagnosisJsonCodec;
        this.diagnosisCompletedEventPublisher = diagnosisCompletedEventPublisher;
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
        List<Long> sessionIds = diagnosisSessionMapper.selectList(Wrappers.<DiagnosisSessionEntity>lambdaQuery()
                        .select(DiagnosisSessionEntity::getId)
                        .eq(DiagnosisSessionEntity::getStatus, DiagnosisSessionStatus.COMPLETED.name())
                        .and(wrapper -> wrapper
                                .in(DiagnosisSessionEntity::getCompletionHooksStatus,
                                        SessionCompletionHookStatus.PENDING.name(),
                                        SessionCompletionHookStatus.FAILED.name())
                                .or(stale -> stale
                                        .eq(DiagnosisSessionEntity::getCompletionHooksStatus, SessionCompletionHookStatus.IN_PROGRESS.name())
                                        .le(DiagnosisSessionEntity::getCompletionHooksUpdatedAt, staleBefore)))
                        .orderByAsc(DiagnosisSessionEntity::getCompletionHooksUpdatedAt)
                        .orderByAsc(DiagnosisSessionEntity::getId)
                        .last("LIMIT " + limit))
                .stream()
                .map(DiagnosisSessionEntity::getId)
                .toList();
        sessionIds.forEach(this::scheduleProcessing);
    }

    private void scheduleProcessing(Long sessionId) {
        try {
            sessionCompletionTaskExecutor.execute(() -> processSafely(sessionId, LocalDateTime.now()));
        } catch (TaskRejectedException exception) {
            log.warn("event=diagnosis_completion_hooks_deferred sessionId={} message={}", sessionId, exception.getMessage());
        }
    }

    private void processSafely(Long sessionId, LocalDateTime staleBefore) {
        if (diagnosisSessionMapper.claimCompletionHooks(sessionId, staleBefore.minus(sessionLifecycleProperties.getPollInterval())) == 0) {
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> processClaimedSession(sessionId));
        } catch (RuntimeException exception) {
            markFailed(sessionId, exception);
            log.warn("event=diagnosis_completion_hooks_failed sessionId={} message={}", sessionId, exception.getMessage(), exception);
        }
    }

    private void processClaimedSession(Long sessionId) {
        DiagnosisSessionEntity session = diagnosisSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Diagnosis session was not found", 404);
        }
        DiagnosisSummaryEntity summary = diagnosisSummaryMapper.selectOne(Wrappers.<DiagnosisSummaryEntity>lambdaQuery()
                .eq(DiagnosisSummaryEntity::getSessionId, sessionId));
        if (summary == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Diagnosis summary was not found", 404);
        }

        diagnosisCompletedEventPublisher.publish(new DiagnosisCompletedEvent(
                session.getId(),
                summary.getId(),
                session.getTemplateId(),
                session.getOwnerUserId(),
                session.getCompletedAt(),
                summary.getPositiveTransferScore().doubleValue(),
                summary.getNegativeTransferRisk().doubleValue(),
                summary.getContextSensitivity().doubleValue(),
                summary.getSemanticDiscrimination().doubleValue(),
                summary.getOverallAccuracy().doubleValue(),
                summary.getAverageReactionTimeMs(),
                diagnosisJsonCodec.readHighRiskLexicalPairs(summary.getHighRiskLexicalPairsJson()).stream()
                        .map(pair -> new DiagnosisCompletedEvent.HighRiskLexicalPairPayload(
                                pair.lexicalPairId(),
                                pair.englishWord(),
                                pair.frenchWord(),
                                pair.riskScore(),
                                pair.dominantErrorType()
                        ))
                        .toList(),
                null,
                1
        ));

        session.setCompletionHooksStatus(SessionCompletionHookStatus.DONE.name());
        session.setCompletionHooksUpdatedAt(LocalDateTime.now());
        session.setCompletionHooksError(null);
        diagnosisSessionMapper.updateById(session);
        log.info("event=diagnosis_completion_hooks_done sessionId={} summaryId={}", sessionId, summary.getId());
    }

    private void markFailed(Long sessionId, RuntimeException exception) {
        DiagnosisSessionEntity session = diagnosisSessionMapper.selectById(sessionId);
        if (session == null) {
            return;
        }
        session.setCompletionHooksStatus(SessionCompletionHookStatus.FAILED.name());
        session.setCompletionHooksUpdatedAt(LocalDateTime.now());
        session.setCompletionHooksError(truncateError(exception.getMessage()));
        diagnosisSessionMapper.updateById(session);
    }

    private String truncateError(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown diagnosis completion hook failure";
        }
        return message.length() <= 512 ? message : message.substring(0, 512);
    }
}
