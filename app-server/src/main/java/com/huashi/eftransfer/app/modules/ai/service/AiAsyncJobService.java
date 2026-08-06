package com.huashi.eftransfer.app.modules.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.modules.ai.dto.ExplainDiagnosisRequest;
import com.huashi.eftransfer.app.modules.ai.dto.LexicalRagQueryRequest;
import com.huashi.eftransfer.app.modules.ai.dto.RecommendTrainingRequest;
import com.huashi.eftransfer.app.modules.ai.entity.AiAsyncJobEntity;
import com.huashi.eftransfer.app.modules.ai.mapper.AiAsyncJobMapper;
import com.huashi.eftransfer.app.modules.ai.support.AiAsyncJobStatus;
import com.huashi.eftransfer.app.modules.ai.support.AiConstants;
import com.huashi.eftransfer.app.modules.ai.support.AiJsonCodec;
import com.huashi.eftransfer.app.modules.ai.vo.AiAsyncJobSubmitVO;
import com.huashi.eftransfer.app.modules.ai.vo.AiAsyncJobVO;
import com.huashi.eftransfer.app.modules.ai.vo.AiGuidanceResponseVO;
import com.huashi.eftransfer.app.modules.ai.vo.LexicalRagAnswerVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AiAsyncJobService {

    private static final Logger log = LoggerFactory.getLogger(AiAsyncJobService.class);

    private final AiAsyncJobMapper aiAsyncJobMapper;
    private final AiInsightService aiInsightService;
    private final LexicalRagQueryService lexicalRagQueryService;
    private final AiJsonCodec aiJsonCodec;
    private final TaskExecutor aiAsyncTaskExecutor;

    public AiAsyncJobService(
            AiAsyncJobMapper aiAsyncJobMapper,
            AiInsightService aiInsightService,
            LexicalRagQueryService lexicalRagQueryService,
            AiJsonCodec aiJsonCodec,
            @Qualifier("aiAsyncTaskExecutor") TaskExecutor aiAsyncTaskExecutor
    ) {
        this.aiAsyncJobMapper = aiAsyncJobMapper;
        this.aiInsightService = aiInsightService;
        this.lexicalRagQueryService = lexicalRagQueryService;
        this.aiJsonCodec = aiJsonCodec;
        this.aiAsyncTaskExecutor = aiAsyncTaskExecutor;
    }

    @Transactional
    public AiAsyncJobSubmitVO submitExplainDiagnosis(ExplainDiagnosisRequest request) {
        return submit(AiConstants.SCENE_EXPLAIN_DIAGNOSIS, request == null ? new ExplainDiagnosisRequest(null) : request);
    }

    @Transactional
    public AiAsyncJobSubmitVO submitRecommendTraining(RecommendTrainingRequest request) {
        return submit(AiConstants.SCENE_RECOMMEND_TRAINING, request == null ? new RecommendTrainingRequest(null) : request);
    }

    @Transactional
    public AiAsyncJobSubmitVO submitLexicalRagQuery(LexicalRagQueryRequest request) {
        if (request == null || request.query() == null || request.query().isBlank()) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, "query must not be blank", 400);
        }
        return submit(AiConstants.SCENE_LEXICAL_RAG_QUERY, request);
    }

    @Transactional(readOnly = true)
    public AiAsyncJobVO getJob(String jobId) {
        Long userId = currentUserId();
        AiAsyncJobEntity entity = aiAsyncJobMapper.selectOne(Wrappers.<AiAsyncJobEntity>lambdaQuery()
                .eq(AiAsyncJobEntity::getJobId, jobId)
                .eq(AiAsyncJobEntity::getUserId, userId)
                .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "AI async job not found", 404);
        }
        return toVo(entity);
    }

    private AiAsyncJobSubmitVO submit(String scene, Object requestPayload) {
        Long userId = currentUserId();
        long activeCount = aiAsyncJobMapper.selectCount(Wrappers.<AiAsyncJobEntity>lambdaQuery()
                .eq(AiAsyncJobEntity::getUserId, userId)
                .eq(AiAsyncJobEntity::getScene, scene)
                .in(AiAsyncJobEntity::getStatus, AiAsyncJobStatus.PENDING, AiAsyncJobStatus.RUNNING));
        if (activeCount > 0) {
            throw new BusinessException(
                    ResultCode.CONFLICT,
                    "An AI job for scene " + scene + " is already in progress",
                    409
            );
        }

        String jobId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        AiAsyncJobEntity entity = new AiAsyncJobEntity();
        entity.setJobId(jobId);
        entity.setUserId(userId);
        entity.setScene(scene);
        entity.setStatus(AiAsyncJobStatus.PENDING);
        entity.setRequestJson(aiJsonCodec.write(requestPayload));
        entity.setCreatedAt(now);
        entity.setCreatedBy(userId);
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(userId);
        entity.setDeleted(false);
        aiAsyncJobMapper.insert(entity);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        scheduleExecution(entity.getId(), authentication);

        return new AiAsyncJobSubmitVO(jobId, scene, AiAsyncJobStatus.PENDING, now);
    }

    private void scheduleExecution(Long entityId, Authentication authentication) {
        Runnable task = () -> executeJob(entityId, authentication);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enqueue(entityId, task);
                }
            });
        } else {
            enqueue(entityId, task);
        }
    }

    private void enqueue(Long entityId, Runnable task) {
        try {
            aiAsyncTaskExecutor.execute(task);
        } catch (RuntimeException exception) {
            log.warn("event=ai_async_job_enqueue_failed entityId={} message={}", entityId, exception.getMessage());
            markFailed(entityId, "AI async executor rejected the job (busy or shutdown)");
        }
    }

    private void executeJob(Long entityId, Authentication authentication) {
        SecurityContext previous = SecurityContextHolder.getContext();
        try {
            if (authentication != null) {
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            }
            AiAsyncJobEntity entity = aiAsyncJobMapper.selectById(entityId);
            if (entity == null) {
                return;
            }
            markRunning(entity);
            Object result = invokeScene(entity);
            markSucceeded(entity, result);
        } catch (Exception exception) {
            log.warn("event=ai_async_job_failed entityId={} message={}", entityId, exception.getMessage(), exception);
            markFailed(entityId, exception.getMessage());
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    private Object invokeScene(AiAsyncJobEntity entity) {
        return switch (entity.getScene()) {
            case AiConstants.SCENE_EXPLAIN_DIAGNOSIS -> aiInsightService.explainDiagnosis(
                    aiJsonCodec.read(entity.getRequestJson(), ExplainDiagnosisRequest.class)
            );
            case AiConstants.SCENE_RECOMMEND_TRAINING -> aiInsightService.recommendTraining(
                    aiJsonCodec.read(entity.getRequestJson(), RecommendTrainingRequest.class)
            );
            case AiConstants.SCENE_LEXICAL_RAG_QUERY -> lexicalRagQueryService.query(
                    aiJsonCodec.read(entity.getRequestJson(), LexicalRagQueryRequest.class)
            );
            default -> throw new IllegalStateException("Unsupported AI async scene: " + entity.getScene());
        };
    }

    private void markRunning(AiAsyncJobEntity entity) {
        entity.setStatus(AiAsyncJobStatus.RUNNING);
        entity.setStartedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        aiAsyncJobMapper.updateById(entity);
    }

    private void markSucceeded(AiAsyncJobEntity entity, Object result) {
        entity.setStatus(AiAsyncJobStatus.SUCCEEDED);
        entity.setResultJson(aiJsonCodec.write(result));
        entity.setErrorMessage(null);
        entity.setFinishedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        aiAsyncJobMapper.updateById(entity);
    }

    private void markFailed(Long entityId, String message) {
        AiAsyncJobEntity entity = aiAsyncJobMapper.selectById(entityId);
        if (entity == null) {
            return;
        }
        entity.setStatus(AiAsyncJobStatus.FAILED);
        entity.setErrorMessage(truncate(message));
        entity.setFinishedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        aiAsyncJobMapper.updateById(entity);
    }

    private AiAsyncJobVO toVo(AiAsyncJobEntity entity) {
        Object result = null;
        if (entity.getResultJson() != null && !entity.getResultJson().isBlank()) {
            result = switch (entity.getScene()) {
                case AiConstants.SCENE_LEXICAL_RAG_QUERY ->
                        aiJsonCodec.read(entity.getResultJson(), LexicalRagAnswerVO.class);
                default -> aiJsonCodec.read(entity.getResultJson(), AiGuidanceResponseVO.class);
            };
        }
        return new AiAsyncJobVO(
                entity.getJobId(),
                entity.getScene(),
                entity.getStatus(),
                result,
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getFinishedAt()
        );
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "AI async job failed";
        }
        String normalized = message.replace('\n', ' ').trim();
        return normalized.length() <= 1000 ? normalized : normalized.substring(0, 1000);
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }
}
