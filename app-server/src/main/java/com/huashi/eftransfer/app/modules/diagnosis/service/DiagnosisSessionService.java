package com.huashi.eftransfer.app.modules.diagnosis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.audit.service.AuditLogService;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.diagnosis.dto.CreateDiagnosisSessionRequest;
import com.huashi.eftransfer.app.modules.diagnosis.dto.DiagnosisSessionPageQuery;
import com.huashi.eftransfer.app.modules.diagnosis.dto.SaveDiagnosisProgressRequest;
import com.huashi.eftransfer.app.modules.diagnosis.dto.SubmitDiagnosisAnswerRequest;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisItemResultEntity;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisSessionEntity;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisSummaryEntity;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisTemplateEntity;
import com.huashi.eftransfer.app.modules.diagnosis.entity.DiagnosisTemplateItemEntity;
import com.huashi.eftransfer.app.modules.diagnosis.event.DiagnosisCompletedEvent;
import com.huashi.eftransfer.app.modules.diagnosis.event.DiagnosisCompletedEventPublisher;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisItemResultMapper;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisSessionMapper;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisSummaryMapper;
import com.huashi.eftransfer.app.modules.diagnosis.mapper.DiagnosisTemplateItemMapper;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisChartPayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisDistributionItem;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisHighRiskLexicalPair;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisJsonCodec;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisOptionPayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisScoringProfilePayload;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisHistorySummaryVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisItemResultDetailVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisNextItemVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisOptionViewVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisQuestionItemVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisResultDetailVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisSessionCreatedVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisSessionProgressVO;
import com.huashi.eftransfer.app.modules.diagnosis.vo.DiagnosisSummaryMetricsVO;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.mapper.LexicalPairMapper;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.DiagnosisAnswerState;
import com.huashi.eftransfer.shared.enums.DiagnosisErrorType;
import com.huashi.eftransfer.shared.enums.DiagnosisSessionStatus;
import com.huashi.eftransfer.shared.enums.DiagnosisTaskType;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageQuery;
import com.huashi.eftransfer.shared.page.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class DiagnosisSessionService {

    private static final Logger log = LoggerFactory.getLogger(DiagnosisSessionService.class);

    private final DiagnosisTemplateService diagnosisTemplateService;
    private final DiagnosisSessionMapper diagnosisSessionMapper;
    private final DiagnosisItemResultMapper diagnosisItemResultMapper;
    private final DiagnosisSummaryMapper diagnosisSummaryMapper;
    private final DiagnosisTemplateItemMapper diagnosisTemplateItemMapper;
    private final LexicalPairMapper lexicalPairMapper;
    private final DiagnosisJsonCodec diagnosisJsonCodec;
    private final DiagnosisScoringPolicy diagnosisScoringPolicy;
    private final DiagnosisCompletedEventPublisher diagnosisCompletedEventPublisher;
    private final AuditLogService auditLogService;

    public DiagnosisSessionService(
            DiagnosisTemplateService diagnosisTemplateService,
            DiagnosisSessionMapper diagnosisSessionMapper,
            DiagnosisItemResultMapper diagnosisItemResultMapper,
            DiagnosisSummaryMapper diagnosisSummaryMapper,
            DiagnosisTemplateItemMapper diagnosisTemplateItemMapper,
            LexicalPairMapper lexicalPairMapper,
            DiagnosisJsonCodec diagnosisJsonCodec,
            DiagnosisScoringPolicy diagnosisScoringPolicy,
            DiagnosisCompletedEventPublisher diagnosisCompletedEventPublisher,
            AuditLogService auditLogService
    ) {
        this.diagnosisTemplateService = diagnosisTemplateService;
        this.diagnosisSessionMapper = diagnosisSessionMapper;
        this.diagnosisItemResultMapper = diagnosisItemResultMapper;
        this.diagnosisSummaryMapper = diagnosisSummaryMapper;
        this.diagnosisTemplateItemMapper = diagnosisTemplateItemMapper;
        this.lexicalPairMapper = lexicalPairMapper;
        this.diagnosisJsonCodec = diagnosisJsonCodec;
        this.diagnosisScoringPolicy = diagnosisScoringPolicy;
        this.diagnosisCompletedEventPublisher = diagnosisCompletedEventPublisher;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public DiagnosisSessionCreatedVO createSession(CreateDiagnosisSessionRequest request) {
        DiagnosisTemplateEntity template = diagnosisTemplateService.requirePublishedTemplate(request.templateId());
        List<DiagnosisTemplateItemEntity> templateItems = diagnosisTemplateService.listTemplateItems(request.templateId());
        if (templateItems.isEmpty()) {
            throw new BusinessException(ResultCode.CONFLICT, "Published diagnosis template does not contain items", 409);
        }
        requireNoActiveSession(currentUserId());

        long seed = ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        List<DiagnosisTemplateItemEntity> orderedItems = orderTemplateItems(templateItems, seed);

        DiagnosisSessionEntity session = new DiagnosisSessionEntity();
        session.setTemplateId(template.getId());
        session.setOwnerUserId(currentUserId());
        session.setStatus(DiagnosisSessionStatus.IN_PROGRESS.name());
        session.setSessionSeed(seed);
        session.setTotalItems(orderedItems.size());
        session.setAnsweredItems(0);
        session.setCurrentItemOrder(orderedItems.isEmpty() ? null : 1);
        session.setStartedAt(LocalDateTime.now());
        diagnosisSessionMapper.insert(session);

        int order = 1;
        for (DiagnosisTemplateItemEntity item : orderedItems) {
            DiagnosisItemResultEntity result = new DiagnosisItemResultEntity();
            result.setSessionId(session.getId());
            result.setTemplateItemId(item.getId());
            result.setLexicalPairId(item.getLexicalPairId());
            result.setTaskType(item.getTaskType());
            result.setPresentationOrder(order++);
            result.setAnswerState(DiagnosisAnswerState.PENDING.name());
            diagnosisItemResultMapper.insert(result);
        }

        auditLogService.record("create_session", "diagnosis_session", String.valueOf(session.getId()), request, ResultCode.SUCCESS.code());
        log.info("event=diagnosis_session_created sessionId={} templateId={} ownerUserId={} totalItems={}",
                session.getId(), template.getId(), session.getOwnerUserId(), session.getTotalItems());
        return new DiagnosisSessionCreatedVO(
                session.getId(),
                template.getId(),
                template.getTemplateName(),
                session.getStatus(),
                session.getTotalItems(),
                session.getAnsweredItems(),
                session.getCurrentItemOrder(),
                session.getStartedAt()
        );
    }

    @Transactional
    public DiagnosisNextItemVO getNextItem(Long sessionId) {
        DiagnosisSessionEntity session = requireAccessibleSession(sessionId);
        session = finalizeStaleSessionIfReady(session);
        if (!DiagnosisSessionStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            return new DiagnosisNextItemVO(
                    session.getId(),
                    session.getStatus(),
                    session.getTotalItems(),
                    session.getAnsweredItems(),
                    session.getCurrentItemOrder(),
                    false,
                    null
            );
        }
        DiagnosisItemResultEntity nextItemResult = findNextPendingItem(session.getId()).orElse(null);
        if (nextItemResult == null) {
            session = finalizeStaleSessionIfReady(session);
            if (DiagnosisSessionStatus.IN_PROGRESS.name().equals(session.getStatus())) {
                session.setCurrentItemOrder(null);
                diagnosisSessionMapper.updateById(session);
            }
            return new DiagnosisNextItemVO(session.getId(), session.getStatus(), session.getTotalItems(), session.getAnsweredItems(), null, false, null);
        }

        if (nextItemResult.getStimulusStartedAt() == null) {
            nextItemResult.setStimulusStartedAt(LocalDateTime.now());
            diagnosisItemResultMapper.updateById(nextItemResult);
        }
        session.setCurrentItemOrder(nextItemResult.getPresentationOrder());
        diagnosisSessionMapper.updateById(session);

        DiagnosisTemplateItemEntity templateItem = requireTemplateItem(nextItemResult.getTemplateItemId());
        LexicalPairEntity lexicalPair = requireLexicalPair(nextItemResult.getLexicalPairId());
        List<DiagnosisOptionViewVO> options = diagnosisJsonCodec.readOptions(templateItem.getOptionsPayloadJson()).stream()
                .map(option -> new DiagnosisOptionViewVO(option.key(), option.label(), option.semanticMatch()))
                .toList();

        DiagnosisQuestionItemVO itemVO = new DiagnosisQuestionItemVO(
                nextItemResult.getId(),
                templateItem.getId(),
                templateItem.getTaskType(),
                nextItemResult.getPresentationOrder(),
                lexicalPair.getId(),
                lexicalPair.getEnglishWord(),
                lexicalPair.getFrenchWord(),
                lexicalPair.getChineseGloss(),
                lexicalPair.getLexicalPairType(),
                templateItem.getContextSupportLevel(),
                diagnosisJsonCodec.readStimulus(templateItem.getStimulusPayloadJson()),
                options
        );
        return new DiagnosisNextItemVO(
                session.getId(),
                session.getStatus(),
                session.getTotalItems(),
                session.getAnsweredItems(),
                session.getCurrentItemOrder(),
                true,
                itemVO
        );
    }

    @Transactional
    public DiagnosisSessionProgressVO submitAnswer(Long sessionId, SubmitDiagnosisAnswerRequest request) {
        DiagnosisSessionEntity session = requireInProgressSession(sessionId);
        DiagnosisItemResultEntity itemResult = requirePendingItemResult(sessionId, request.itemResultId());
        validateAnswerTimings(request);

        DiagnosisTemplateItemEntity templateItem = requireTemplateItem(itemResult.getTemplateItemId());
        LexicalPairEntity lexicalPair = requireLexicalPair(itemResult.getLexicalPairId());
        validateTaskSpecificAnswer(templateItem, request);

        DiagnosisScoringPolicy.ItemDefinition itemDefinition = toItemDefinition(templateItem, lexicalPair);
        DiagnosisScoringPolicy.ItemEvaluation evaluation;
        try {
            evaluation = diagnosisScoringPolicy.evaluate(
                    itemDefinition,
                    new DiagnosisScoringPolicy.SubmittedAnswer(
                            request.selectedSemanticMatch(),
                            request.selectedAnswerKey(),
                            request.reactionTimeMs(),
                            request.hesitationTimeMs()
                    )
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, exception.getMessage());
        }

        if (itemResult.getStimulusStartedAt() == null) {
            itemResult.setStimulusStartedAt(LocalDateTime.now());
        }
        itemResult.setAnswerState(DiagnosisAnswerState.ANSWERED.name());
        itemResult.setSubmittedAt(LocalDateTime.now());
        itemResult.setReactionTimeMs(request.reactionTimeMs());
        itemResult.setHesitationTimeMs(request.hesitationTimeMs());
        itemResult.setSelectedAnswerKey(evaluation.selectedAnswerKey());
        DiagnosisOptionPayload selectedOption = findOptionByKey(itemDefinition.options(), evaluation.selectedAnswerKey());
        Map<String, Object> answerPayload = new LinkedHashMap<>();
        answerPayload.put("selectedSemanticMatch",
                request.selectedSemanticMatch() != null
                        ? request.selectedSemanticMatch()
                        : selectedOption == null ? null : selectedOption.semanticMatch());
        answerPayload.put("selectedAnswerKey", evaluation.selectedAnswerKey());
        answerPayload.put("reactionTimeMs", request.reactionTimeMs());
        answerPayload.put("hesitationTimeMs", request.hesitationTimeMs());
        itemResult.setAnswerPayloadJson(diagnosisJsonCodec.write(answerPayload));
        itemResult.setIsCorrect(evaluation.correct());
        itemResult.setDetectedErrorType(evaluation.errorType() == null ? null : evaluation.errorType().name());
        itemResult.setSemanticConsistent(evaluation.semanticConsistent());
        itemResult.setTransferRiskScore(decimal(evaluation.transferRiskScore()));
        itemResult.setItemScore(decimal(evaluation.itemScore()));
        diagnosisItemResultMapper.updateById(itemResult);

        session.setAnsweredItems(session.getAnsweredItems() + 1);
        Optional<DiagnosisItemResultEntity> nextPendingItem = findNextPendingItem(session.getId());
        session.setCurrentItemOrder(nextPendingItem.map(DiagnosisItemResultEntity::getPresentationOrder).orElse(null));
        if (nextPendingItem.isEmpty()) {
            session = finalizeCompletedSession(session);
        } else {
            diagnosisSessionMapper.updateById(session);
        }

        auditLogService.record("submit_answer", "diagnosis_item_result", String.valueOf(itemResult.getId()), request, ResultCode.SUCCESS.code());
        log.info("event=diagnosis_answer_submitted sessionId={} itemResultId={} correct={} errorType={} answeredItems={}/{}",
                sessionId,
                itemResult.getId(),
                evaluation.correct(),
                evaluation.errorType() == null ? null : evaluation.errorType().code(),
                session.getAnsweredItems(),
                session.getTotalItems());
        return progressVO(session);
    }

    @Transactional
    public DiagnosisSessionProgressVO saveProgress(Long sessionId, SaveDiagnosisProgressRequest request) {
        DiagnosisSessionEntity session = requireAccessibleSession(sessionId);
        session = finalizeStaleSessionIfReady(session);
        if (DiagnosisSessionStatus.COMPLETED.name().equals(session.getStatus())) {
            return progressVO(session);
        }
        if (!DiagnosisSessionStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Diagnosis session is not in progress", 409);
        }
        session.setProgressSnapshotJson(diagnosisJsonCodec.write(request.progressSnapshot()));
        session.setLastSavedAt(LocalDateTime.now());
        diagnosisSessionMapper.updateById(session);
        auditLogService.record("save_progress", "diagnosis_session", String.valueOf(sessionId), request, ResultCode.SUCCESS.code());
        log.info("event=diagnosis_progress_saved sessionId={} answeredItems={}/{}", sessionId, session.getAnsweredItems(), session.getTotalItems());
        return progressVO(session);
    }

    @Transactional
    public DiagnosisSessionProgressVO completeSession(Long sessionId) {
        DiagnosisSessionEntity session = requireAccessibleSession(sessionId);
        session = finalizeStaleSessionIfReady(session);
        if (DiagnosisSessionStatus.COMPLETED.name().equals(session.getStatus())) {
            return progressVO(session);
        }
        if (!DiagnosisSessionStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Diagnosis session is not in progress", 409);
        }
        if (!isSessionReadyToComplete(session)) {
            throw new BusinessException(ResultCode.CONFLICT, "Diagnosis session is not fully answered", 409);
        }
        session = finalizeCompletedSession(session);
        return progressVO(session);
    }

    public DiagnosisResultDetailVO getResultDetail(Long sessionId) {
        DiagnosisSessionEntity session = requireAccessibleSession(sessionId);
        session = finalizeStaleSessionIfReady(session);
        if (!DiagnosisSessionStatus.COMPLETED.name().equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Diagnosis session is not completed", 409);
        }
        DiagnosisSummaryEntity summary = requireSummary(sessionId);
        DiagnosisTemplateEntity template = diagnosisTemplateService.requireExistingTemplate(session.getTemplateId());

        List<DiagnosisItemResultEntity> itemResults = diagnosisItemResultMapper.selectList(Wrappers.<DiagnosisItemResultEntity>lambdaQuery()
                .eq(DiagnosisItemResultEntity::getSessionId, sessionId)
                .orderByAsc(DiagnosisItemResultEntity::getPresentationOrder)
                .orderByAsc(DiagnosisItemResultEntity::getId));
        Map<Long, DiagnosisTemplateItemEntity> templateItemMap = loadTemplateItemMap(itemResults.stream().map(DiagnosisItemResultEntity::getTemplateItemId).toList());
        Map<Long, LexicalPairEntity> lexicalPairMap = loadLexicalPairMap(itemResults.stream().map(DiagnosisItemResultEntity::getLexicalPairId).toList());

        List<DiagnosisItemResultDetailVO> items = itemResults.stream()
                .map(itemResult -> toItemResultDetailVO(itemResult, templateItemMap.get(itemResult.getTemplateItemId()), lexicalPairMap.get(itemResult.getLexicalPairId())))
                .toList();

        return new DiagnosisResultDetailVO(
                session.getId(),
                session.getStatus(),
                template.getId(),
                template.getTemplateName(),
                session.getOwnerUserId(),
                session.getTotalItems(),
                session.getAnsweredItems(),
                session.getStartedAt(),
                session.getCompletedAt(),
                new DiagnosisSummaryMetricsVO(
                        summary.getPositiveTransferScore().doubleValue(),
                        summary.getNegativeTransferRisk().doubleValue(),
                        summary.getContextSensitivity().doubleValue(),
                        summary.getSemanticDiscrimination().doubleValue(),
                        summary.getOverallAccuracy().doubleValue(),
                        summary.getAverageReactionTimeMs()
                ),
                diagnosisJsonCodec.readDistributionItems(summary.getErrorTypeDistributionJson()),
                diagnosisJsonCodec.readHighRiskLexicalPairs(summary.getHighRiskLexicalPairsJson()),
                diagnosisJsonCodec.readChartPayload(summary.getChartPayloadJson()),
                items
        );
    }

    public PageResult<DiagnosisHistorySummaryVO> pageHistory(DiagnosisSessionPageQuery query) {
        PageQuery pageQuery = query.toPageQuery();
        LambdaQueryWrapper<DiagnosisSessionEntity> wrapper = Wrappers.<DiagnosisSessionEntity>lambdaQuery()
                .orderByDesc(DiagnosisSessionEntity::getStartedAt)
                .orderByDesc(DiagnosisSessionEntity::getId);

        if (query.status() != null && !query.status().isBlank()) {
            wrapper.eq(DiagnosisSessionEntity::getStatus, parseSessionStatus(query.status()).name());
        }
        if (query.templateId() != null) {
            wrapper.eq(DiagnosisSessionEntity::getTemplateId, query.templateId());
        }

        Long ownerFilter = resolveHistoryOwnerFilter(query);
        if (shouldHealInProgressHistory(query, ownerFilter)) {
            healStaleInProgressSessions(ownerFilter);
        }
        if (ownerFilter != null) {
            wrapper.eq(DiagnosisSessionEntity::getOwnerUserId, ownerFilter);
        }

        long total = diagnosisSessionMapper.selectCount(wrapper);
        List<DiagnosisSessionEntity> sessions = diagnosisSessionMapper.selectList(wrapper
                .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()));
        if (sessions.isEmpty()) {
            return new PageResult<>(total, pageQuery.pageNo(), pageQuery.pageSize(), List.of());
        }

        Map<Long, DiagnosisTemplateEntity> templateMap = sessions.stream()
                .map(DiagnosisSessionEntity::getTemplateId)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .map(diagnosisTemplateService::requireExistingTemplate)
                .collect(Collectors.toMap(DiagnosisTemplateEntity::getId, template -> template));
        Map<Long, DiagnosisSummaryEntity> summaryMap = diagnosisSummaryMapper.selectList(Wrappers.<DiagnosisSummaryEntity>lambdaQuery()
                        .in(DiagnosisSummaryEntity::getSessionId, sessions.stream().map(DiagnosisSessionEntity::getId).toList()))
                .stream()
                .collect(Collectors.toMap(DiagnosisSummaryEntity::getSessionId, summary -> summary));

        List<DiagnosisHistorySummaryVO> records = sessions.stream()
                .map(session -> {
                    DiagnosisTemplateEntity template = templateMap.get(session.getTemplateId());
                    DiagnosisSummaryEntity summary = summaryMap.get(session.getId());
                    return new DiagnosisHistorySummaryVO(
                            session.getId(),
                            session.getTemplateId(),
                            template == null ? null : template.getTemplateName(),
                            session.getOwnerUserId(),
                            session.getStatus(),
                            session.getStartedAt(),
                            session.getCompletedAt(),
                            summary == null ? null : summary.getPositiveTransferScore().doubleValue(),
                            summary == null ? null : summary.getNegativeTransferRisk().doubleValue(),
                            summary == null ? null : summary.getOverallAccuracy().doubleValue()
                    );
                })
                .toList();

        return new PageResult<>(total, pageQuery.pageNo(), pageQuery.pageSize(), records);
    }

    private DiagnosisSummaryEntity upsertSummary(
            DiagnosisSessionEntity session,
            DiagnosisTemplateEntity template,
            DiagnosisScoringPolicy.SummaryAggregation aggregation
    ) {
        DiagnosisSummaryEntity existing = diagnosisSummaryMapper.selectOne(Wrappers.<DiagnosisSummaryEntity>lambdaQuery()
                .eq(DiagnosisSummaryEntity::getSessionId, session.getId()));
        DiagnosisSummaryEntity entity = existing == null ? new DiagnosisSummaryEntity() : existing;
        entity.setSessionId(session.getId());
        entity.setOwnerUserId(session.getOwnerUserId());
        entity.setTemplateId(template.getId());
        entity.setPositiveTransferScore(decimal(aggregation.positiveTransferScore()));
        entity.setNegativeTransferRisk(decimal(aggregation.negativeTransferRisk()));
        entity.setContextSensitivity(decimal(aggregation.contextSensitivity()));
        entity.setSemanticDiscrimination(decimal(aggregation.semanticDiscrimination()));
        entity.setOverallAccuracy(decimal(aggregation.overallAccuracy()));
        entity.setAverageReactionTimeMs(aggregation.averageReactionTime());
        entity.setErrorTypeDistributionJson(diagnosisJsonCodec.write(aggregation.errorTypeDistribution()));
        entity.setHighRiskLexicalPairsJson(diagnosisJsonCodec.write(aggregation.highRiskLexicalPairs()));
        entity.setChartPayloadJson(diagnosisJsonCodec.write(aggregation.chartPayload()));
        entity.setGeneratedAt(LocalDateTime.now());
        entity.setScoringVersion(template.getScoringVersion());

        if (existing == null) {
            diagnosisSummaryMapper.insert(entity);
        } else {
            diagnosisSummaryMapper.updateById(entity);
        }
        return entity;
    }

    private void registerAfterCommitEvent(
            DiagnosisSessionEntity session,
            DiagnosisSummaryEntity summary,
            DiagnosisScoringPolicy.SummaryAggregation aggregation
    ) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            publishCompletedEvent(session, summary, aggregation);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishCompletedEvent(session, summary, aggregation);
            }
        });
    }

    private void publishCompletedEvent(
            DiagnosisSessionEntity session,
            DiagnosisSummaryEntity summary,
            DiagnosisScoringPolicy.SummaryAggregation aggregation
    ) {
        DiagnosisCompletedEvent event = new DiagnosisCompletedEvent(
                session.getId(),
                summary.getId(),
                session.getTemplateId(),
                session.getOwnerUserId(),
                session.getCompletedAt(),
                aggregation.positiveTransferScore(),
                aggregation.negativeTransferRisk(),
                aggregation.contextSensitivity(),
                aggregation.semanticDiscrimination(),
                aggregation.overallAccuracy(),
                aggregation.averageReactionTime(),
                aggregation.highRiskLexicalPairs().stream()
                        .map(pair -> new DiagnosisCompletedEvent.HighRiskLexicalPairPayload(
                                pair.lexicalPairId(),
                                pair.englishWord(),
                                pair.frenchWord(),
                                pair.riskScore(),
                                pair.dominantErrorType()
                        ))
                        .toList(),
                MDC.get("traceId"),
                1
        );
        diagnosisCompletedEventPublisher.publish(event);
    }

    private DiagnosisItemResultDetailVO toItemResultDetailVO(
            DiagnosisItemResultEntity itemResult,
            DiagnosisTemplateItemEntity templateItem,
            LexicalPairEntity lexicalPair
    ) {
        List<DiagnosisOptionPayload> options = diagnosisJsonCodec.readOptions(templateItem.getOptionsPayloadJson());
        return new DiagnosisItemResultDetailVO(
                itemResult.getId(),
                templateItem.getId(),
                itemResult.getPresentationOrder(),
                templateItem.getTaskType(),
                lexicalPair.getId(),
                lexicalPair.getEnglishWord(),
                lexicalPair.getFrenchWord(),
                lexicalPair.getChineseGloss(),
                lexicalPair.getLexicalPairType(),
                templateItem.getContextSupportLevel(),
                templateItem.getExpectedSemanticMatch(),
                diagnosisJsonCodec.readStimulus(templateItem.getStimulusPayloadJson()),
                options,
                templateItem.getCorrectAnswerKey(),
                itemResult.getSelectedAnswerKey(),
                itemResult.getReactionTimeMs(),
                itemResult.getHesitationTimeMs(),
                itemResult.getIsCorrect(),
                itemResult.getSemanticConsistent(),
                itemResult.getDetectedErrorType() == null ? null : DiagnosisErrorType.valueOf(itemResult.getDetectedErrorType()).code(),
                itemResult.getTransferRiskScore() == null ? null : itemResult.getTransferRiskScore().doubleValue(),
                itemResult.getItemScore() == null ? null : itemResult.getItemScore().doubleValue()
        );
    }

    private DiagnosisScoringPolicy.ItemDefinition toItemDefinition(
            DiagnosisTemplateItemEntity templateItem,
            LexicalPairEntity lexicalPair
    ) {
        return new DiagnosisScoringPolicy.ItemDefinition(
                lexicalPair.getId(),
                lexicalPair.getLexicalPairType(),
                templateItem.getTaskType(),
                templateItem.getContextSupportLevel(),
                Boolean.TRUE.equals(templateItem.getExpectedSemanticMatch()),
                templateItem.getCorrectAnswerKey(),
                diagnosisJsonCodec.readOptions(templateItem.getOptionsPayloadJson()),
                diagnosisJsonCodec.readScoringProfile(templateItem.getScoringProfileJson()),
                lexicalPair.getEnglishWord(),
                lexicalPair.getFrenchWord(),
                lexicalPair.getFalseFriendRisk().doubleValue(),
                lexicalPair.getSemanticOverlapScore().doubleValue()
        );
    }

    private Optional<DiagnosisItemResultEntity> findNextPendingItem(Long sessionId) {
        return diagnosisItemResultMapper.selectList(Wrappers.<DiagnosisItemResultEntity>lambdaQuery()
                        .eq(DiagnosisItemResultEntity::getSessionId, sessionId)
                        .eq(DiagnosisItemResultEntity::getAnswerState, DiagnosisAnswerState.PENDING.name())
                        .orderByAsc(DiagnosisItemResultEntity::getPresentationOrder)
                        .last("LIMIT 1"))
                .stream()
                .findFirst();
    }

    private DiagnosisSessionEntity requireAccessibleSession(Long sessionId) {
        DiagnosisSessionEntity session = diagnosisSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Diagnosis session was not found", 404);
        }
        if (!isAdmin() && !Objects.equals(session.getOwnerUserId(), currentUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "You do not have permission to access this diagnosis session", 403);
        }
        return session;
    }

    private void requireNoActiveSession(Long ownerUserId) {
        List<DiagnosisSessionEntity> activeSessions = diagnosisSessionMapper.selectList(Wrappers.<DiagnosisSessionEntity>lambdaQuery()
                .eq(DiagnosisSessionEntity::getOwnerUserId, ownerUserId)
                .eq(DiagnosisSessionEntity::getStatus, DiagnosisSessionStatus.IN_PROGRESS.name())
                .orderByDesc(DiagnosisSessionEntity::getStartedAt)
                .orderByDesc(DiagnosisSessionEntity::getId));
        for (DiagnosisSessionEntity existing : activeSessions) {
            DiagnosisSessionEntity healedSession = finalizeStaleSessionIfReady(existing);
            if (DiagnosisSessionStatus.COMPLETED.name().equals(healedSession.getStatus())) {
                continue;
            }
            throw new BusinessException(
                    ResultCode.CONFLICT,
                    "Diagnosis session already in progress. Resume the active session before starting a new one.",
                    409
            );
        }
    }

    private DiagnosisSessionEntity requireInProgressSession(Long sessionId) {
        DiagnosisSessionEntity session = requireAccessibleSession(sessionId);
        session = finalizeStaleSessionIfReady(session);
        if (!DiagnosisSessionStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "Diagnosis session is not in progress", 409);
        }
        return session;
    }

    private DiagnosisItemResultEntity requirePendingItemResult(Long sessionId, Long itemResultId) {
        DiagnosisItemResultEntity itemResult = diagnosisItemResultMapper.selectOne(Wrappers.<DiagnosisItemResultEntity>lambdaQuery()
                .eq(DiagnosisItemResultEntity::getId, itemResultId)
                .eq(DiagnosisItemResultEntity::getSessionId, sessionId));
        if (itemResult == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Diagnosis item result was not found", 404);
        }
        if (!DiagnosisAnswerState.PENDING.name().equals(itemResult.getAnswerState())) {
            throw new BusinessException(ResultCode.CONFLICT, "Diagnosis item has already been answered", 409);
        }
        return itemResult;
    }

    private DiagnosisTemplateItemEntity requireTemplateItem(Long templateItemId) {
        DiagnosisTemplateItemEntity item = diagnosisTemplateItemMapper.selectById(templateItemId);
        if (item == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Diagnosis template item was not found", 404);
        }
        return item;
    }

    private LexicalPairEntity requireLexicalPair(Long lexicalPairId) {
        LexicalPairEntity lexicalPair = lexicalPairMapper.selectById(lexicalPairId);
        if (lexicalPair == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Lexical pair was not found", 404);
        }
        return lexicalPair;
    }

    private DiagnosisSummaryEntity requireSummary(Long sessionId) {
        DiagnosisSummaryEntity summary = diagnosisSummaryMapper.selectOne(Wrappers.<DiagnosisSummaryEntity>lambdaQuery()
                .eq(DiagnosisSummaryEntity::getSessionId, sessionId));
        if (summary == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Diagnosis summary was not found", 404);
        }
        return summary;
    }

    private List<DiagnosisTemplateItemEntity> orderTemplateItems(List<DiagnosisTemplateItemEntity> items, long seed) {
        Map<String, List<DiagnosisTemplateItemEntity>> grouped = items.stream()
                .sorted(Comparator.comparing(DiagnosisTemplateItemEntity::getSortOrder).thenComparing(DiagnosisTemplateItemEntity::getId))
                .collect(Collectors.groupingBy(DiagnosisTemplateItemEntity::getBlockCode, LinkedHashMap::new, Collectors.toList()));

        List<DiagnosisTemplateItemEntity> ordered = new ArrayList<>();
        int blockIndex = 0;
        for (List<DiagnosisTemplateItemEntity> blockItems : grouped.values()) {
            List<DiagnosisTemplateItemEntity> shuffled = new ArrayList<>(blockItems);
            Collections.shuffle(shuffled, new java.util.Random(seed + (++blockIndex * 31L)));
            ordered.addAll(shuffled);
        }
        return ordered;
    }

    private Map<Long, DiagnosisTemplateItemEntity> loadTemplateItemMap(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Set<Long> uniqueIds = ids.stream().collect(Collectors.toCollection(LinkedHashSet::new));
        List<DiagnosisTemplateItemEntity> items = diagnosisTemplateItemMapper.selectBatchIds(uniqueIds);
        if (items.size() != uniqueIds.size()) {
            Set<Long> existingIds = items.stream().map(DiagnosisTemplateItemEntity::getId).collect(Collectors.toSet());
            Long missingId = uniqueIds.stream().filter(id -> !existingIds.contains(id)).findFirst().orElse(null);
            throw new BusinessException(ResultCode.NOT_FOUND, "Diagnosis template item was not found: " + missingId, 404);
        }
        return items
                .stream()
                .collect(Collectors.toMap(DiagnosisTemplateItemEntity::getId, item -> item));
    }

    private Map<Long, LexicalPairEntity> loadLexicalPairMap(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Set<Long> uniqueIds = ids.stream().collect(Collectors.toCollection(LinkedHashSet::new));
        List<LexicalPairEntity> pairs = lexicalPairMapper.selectBatchIds(uniqueIds);
        if (pairs.size() != uniqueIds.size()) {
            Set<Long> existingIds = pairs.stream().map(LexicalPairEntity::getId).collect(Collectors.toSet());
            Long missingId = uniqueIds.stream().filter(id -> !existingIds.contains(id)).findFirst().orElse(null);
            throw new BusinessException(ResultCode.NOT_FOUND, "Lexical pair was not found: " + missingId, 404);
        }
        return pairs.stream().collect(Collectors.toMap(LexicalPairEntity::getId, pair -> pair));
    }

    private void validateTaskSpecificAnswer(DiagnosisTemplateItemEntity templateItem, SubmitDiagnosisAnswerRequest request) {
        DiagnosisTaskType taskType = DiagnosisTaskType.fromCode(templateItem.getTaskType());
        List<DiagnosisOptionPayload> options = diagnosisJsonCodec.readOptions(templateItem.getOptionsPayloadJson());
        if (taskType == DiagnosisTaskType.REACTION_TIME) {
            if (isBlank(request.selectedAnswerKey()) && request.selectedSemanticMatch() == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "Reaction time task requires selectedAnswerKey or selectedSemanticMatch");
            }
            if (!isBlank(request.selectedAnswerKey())) {
                DiagnosisOptionPayload selectedOption = findOptionByKey(options, request.selectedAnswerKey());
                if (selectedOption == null) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "Selected answer key is not defined in this diagnosis item");
                }
                if (request.selectedSemanticMatch() != null
                        && selectedOption.semanticMatch() != null
                        && !Objects.equals(selectedOption.semanticMatch(), request.selectedSemanticMatch())) {
                    throw new BusinessException(ResultCode.BAD_REQUEST, "selectedSemanticMatch does not align with selectedAnswerKey");
                }
            }
            return;
        }

        if (isBlank(request.selectedAnswerKey())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Semantic judgement task requires selectedAnswerKey");
        }
        if (findOptionByKey(options, request.selectedAnswerKey()) == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Selected answer key is not defined in this diagnosis item");
        }
    }

    private void validateAnswerTimings(SubmitDiagnosisAnswerRequest request) {
        if (request.hesitationTimeMs() > request.reactionTimeMs()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "hesitationTimeMs must not exceed reactionTimeMs");
        }
    }

    private DiagnosisOptionPayload findOptionByKey(List<DiagnosisOptionPayload> options, String selectedAnswerKey) {
        return options.stream()
                .filter(option -> option.key() != null && option.key().equalsIgnoreCase(selectedAnswerKey.trim()))
                .findFirst()
                .orElse(null);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private DiagnosisSessionEntity finalizeCompletedSession(DiagnosisSessionEntity session) {
        if (DiagnosisSessionStatus.COMPLETED.name().equals(session.getStatus())) {
            return session;
        }
        if (!isSessionReadyToComplete(session)) {
            throw new BusinessException(ResultCode.CONFLICT, "Diagnosis session is not fully answered", 409);
        }

        DiagnosisTemplateEntity template = diagnosisTemplateService.requireExistingTemplate(session.getTemplateId());
        List<DiagnosisItemResultEntity> itemResults = diagnosisItemResultMapper.selectList(Wrappers.<DiagnosisItemResultEntity>lambdaQuery()
                .eq(DiagnosisItemResultEntity::getSessionId, session.getId())
                .orderByAsc(DiagnosisItemResultEntity::getPresentationOrder)
                .orderByAsc(DiagnosisItemResultEntity::getId));
        Map<Long, DiagnosisTemplateItemEntity> templateItemMap = loadTemplateItemMap(itemResults.stream().map(DiagnosisItemResultEntity::getTemplateItemId).toList());
        Map<Long, LexicalPairEntity> lexicalPairMap = loadLexicalPairMap(itemResults.stream().map(DiagnosisItemResultEntity::getLexicalPairId).toList());

        List<DiagnosisScoringPolicy.AnsweredItem> answeredItems = itemResults.stream()
                .map(itemResult -> {
                    DiagnosisTemplateItemEntity templateItem = templateItemMap.get(itemResult.getTemplateItemId());
                    LexicalPairEntity lexicalPair = lexicalPairMap.get(itemResult.getLexicalPairId());
                    return new DiagnosisScoringPolicy.AnsweredItem(
                            itemResult.getId(),
                            itemResult.getPresentationOrder(),
                            toItemDefinition(templateItem, lexicalPair),
                            itemResult.getSelectedAnswerKey(),
                            itemResult.getReactionTimeMs(),
                            itemResult.getHesitationTimeMs(),
                            Boolean.TRUE.equals(itemResult.getIsCorrect()),
                            Boolean.TRUE.equals(itemResult.getSemanticConsistent()),
                            itemResult.getDetectedErrorType() == null ? null : DiagnosisErrorType.valueOf(itemResult.getDetectedErrorType()),
                            itemResult.getTransferRiskScore() == null ? 0 : itemResult.getTransferRiskScore().doubleValue(),
                            itemResult.getItemScore() == null ? 0 : itemResult.getItemScore().doubleValue()
                    );
                })
                .toList();

        DiagnosisScoringPolicy.SummaryAggregation aggregation = diagnosisScoringPolicy.aggregate(answeredItems);
        DiagnosisSummaryEntity summary = upsertSummary(session, template, aggregation);

        session.setStatus(DiagnosisSessionStatus.COMPLETED.name());
        session.setCompletedAt(LocalDateTime.now());
        session.setCurrentItemOrder(null);
        diagnosisSessionMapper.updateById(session);

        registerAfterCommitEvent(session, summary, aggregation);
        auditLogService.record("complete_session", "diagnosis_session", String.valueOf(session.getId()), Map.of("sessionId", session.getId()), ResultCode.SUCCESS.code());
        log.info("event=diagnosis_session_completed sessionId={} summaryId={} positiveTransferScore={} negativeTransferRisk={}",
                session.getId(), summary.getId(), aggregation.positiveTransferScore(), aggregation.negativeTransferRisk());
        return session;
    }

    private DiagnosisSessionEntity finalizeStaleSessionIfReady(DiagnosisSessionEntity session) {
        if (session == null || !DiagnosisSessionStatus.IN_PROGRESS.name().equals(session.getStatus())) {
            return session;
        }
        int answeredCount = countAnsweredItems(session.getId());
        if (!Objects.equals(session.getAnsweredItems(), answeredCount)) {
            session.setAnsweredItems(answeredCount);
        }
        if (!isSessionReadyToComplete(session, answeredCount)) {
            return session;
        }
        return finalizeCompletedSession(session);
    }

    private boolean isSessionReadyToComplete(DiagnosisSessionEntity session) {
        return isSessionReadyToComplete(session, countAnsweredItems(session.getId()));
    }

    private boolean isSessionReadyToComplete(DiagnosisSessionEntity session, int answeredCount) {
        return countPendingItems(session.getId()) == 0 && answeredCount >= session.getTotalItems();
    }

    private int countAnsweredItems(Long sessionId) {
        return Math.toIntExact(diagnosisItemResultMapper.selectCount(Wrappers.<DiagnosisItemResultEntity>lambdaQuery()
                .eq(DiagnosisItemResultEntity::getSessionId, sessionId)
                .eq(DiagnosisItemResultEntity::getAnswerState, DiagnosisAnswerState.ANSWERED.name())));
    }

    private long countPendingItems(Long sessionId) {
        return diagnosisItemResultMapper.selectCount(Wrappers.<DiagnosisItemResultEntity>lambdaQuery()
                .eq(DiagnosisItemResultEntity::getSessionId, sessionId)
                .eq(DiagnosisItemResultEntity::getAnswerState, DiagnosisAnswerState.PENDING.name()));
    }

    private DiagnosisSessionProgressVO progressVO(DiagnosisSessionEntity session) {
        return new DiagnosisSessionProgressVO(
                session.getId(),
                session.getStatus(),
                session.getTotalItems(),
                session.getAnsweredItems(),
                session.getCurrentItemOrder(),
                DiagnosisSessionStatus.COMPLETED.name().equals(session.getStatus())
        );
    }

    private Long resolveHistoryOwnerFilter(DiagnosisSessionPageQuery query) {
        if (isAdmin()) {
            if (query.ownerUserId() != null) {
                return query.ownerUserId();
            }
            return Boolean.TRUE.equals(query.mineOnly()) ? currentUserId() : null;
        }
        return currentUserId();
    }

    private boolean shouldHealInProgressHistory(DiagnosisSessionPageQuery query, Long ownerFilter) {
        if (ownerFilter == null) {
            return false;
        }
        if (query.status() == null || query.status().isBlank()) {
            return true;
        }
        return parseSessionStatus(query.status()) == DiagnosisSessionStatus.IN_PROGRESS;
    }

    private void healStaleInProgressSessions(Long ownerUserId) {
        diagnosisSessionMapper.selectList(Wrappers.<DiagnosisSessionEntity>lambdaQuery()
                        .eq(DiagnosisSessionEntity::getOwnerUserId, ownerUserId)
                        .eq(DiagnosisSessionEntity::getStatus, DiagnosisSessionStatus.IN_PROGRESS.name())
                        .orderByDesc(DiagnosisSessionEntity::getStartedAt)
                        .orderByDesc(DiagnosisSessionEntity::getId))
                .forEach(this::finalizeStaleSessionIfReady);
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

    private BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private DiagnosisSessionStatus parseSessionStatus(String value) {
        try {
            String normalized = value.trim();
            if ("in_progress".equalsIgnoreCase(normalized) || "completed".equalsIgnoreCase(normalized)) {
                return DiagnosisSessionStatus.valueOf(normalized.toUpperCase());
            }
            return DiagnosisSessionStatus.valueOf(normalized.toUpperCase());
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "Unsupported diagnosisSessionStatus: " + value, 400);
        }
    }
}
