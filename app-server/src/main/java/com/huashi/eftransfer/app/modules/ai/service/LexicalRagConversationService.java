package com.huashi.eftransfer.app.modules.ai.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.huashi.eftransfer.app.modules.ai.dto.LexicalRagConversationPageQuery;
import com.huashi.eftransfer.app.modules.ai.entity.LexicalRagConversationMessageEntity;
import com.huashi.eftransfer.app.modules.ai.entity.LexicalRagConversationSessionEntity;
import com.huashi.eftransfer.app.modules.ai.mapper.LexicalRagConversationMessageMapper;
import com.huashi.eftransfer.app.modules.ai.mapper.LexicalRagConversationSessionMapper;
import com.huashi.eftransfer.app.modules.ai.support.AiConstants;
import com.huashi.eftransfer.app.modules.ai.support.AiJsonCodec;
import com.huashi.eftransfer.app.modules.ai.vo.LexicalRagAnswerVO;
import com.huashi.eftransfer.app.modules.ai.vo.LexicalRagConversationDetailVO;
import com.huashi.eftransfer.app.modules.ai.vo.LexicalRagConversationMessageVO;
import com.huashi.eftransfer.app.modules.ai.vo.LexicalRagConversationSummaryVO;
import com.huashi.eftransfer.shared.ai.ChatMessage;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageQuery;
import com.huashi.eftransfer.shared.page.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class LexicalRagConversationService {

    private static final int DEFAULT_CONTEXT_MESSAGE_LIMIT = 12;

    private final LexicalRagConversationSessionMapper conversationSessionMapper;
    private final LexicalRagConversationMessageMapper conversationMessageMapper;
    private final AiJsonCodec aiJsonCodec;

    public LexicalRagConversationService(
            LexicalRagConversationSessionMapper conversationSessionMapper,
            LexicalRagConversationMessageMapper conversationMessageMapper,
            AiJsonCodec aiJsonCodec
    ) {
        this.conversationSessionMapper = conversationSessionMapper;
        this.conversationMessageMapper = conversationMessageMapper;
        this.aiJsonCodec = aiJsonCodec;
    }

    @Transactional(readOnly = true)
    public PageResult<LexicalRagConversationSummaryVO> pageMine(Long studentUserId, LexicalRagConversationPageQuery query) {
        PageQuery pageQuery = query.toPageQuery();
        long total = conversationSessionMapper.selectCount(Wrappers.<LexicalRagConversationSessionEntity>lambdaQuery()
                .eq(LexicalRagConversationSessionEntity::getStudentUserId, studentUserId)
                .eq(LexicalRagConversationSessionEntity::getScene, AiConstants.SCENE_LEXICAL_RAG_QUERY));
        if (total == 0) {
            return new PageResult<>(0, pageQuery.pageNo(), pageQuery.pageSize(), List.of());
        }
        List<LexicalRagConversationSummaryVO> records = conversationSessionMapper.selectList(Wrappers.<LexicalRagConversationSessionEntity>lambdaQuery()
                        .eq(LexicalRagConversationSessionEntity::getStudentUserId, studentUserId)
                        .eq(LexicalRagConversationSessionEntity::getScene, AiConstants.SCENE_LEXICAL_RAG_QUERY)
                        .orderByDesc(LexicalRagConversationSessionEntity::getLastMessageAt)
                        .orderByDesc(LexicalRagConversationSessionEntity::getId)
                        .last("LIMIT " + pageQuery.pageSize() + " OFFSET " + pageQuery.offset()))
                .stream()
                .map(entity -> new LexicalRagConversationSummaryVO(
                        entity.getConversationId(),
                        entity.getTitle(),
                        entity.getLastMessageAt()
                ))
                .toList();
        return new PageResult<>(total, pageQuery.pageNo(), pageQuery.pageSize(), records);
    }

    @Transactional(readOnly = true)
    public LexicalRagConversationDetailVO detail(Long studentUserId, String conversationId) {
        LexicalRagConversationSessionEntity session = requireConversation(studentUserId, conversationId);
        List<LexicalRagConversationMessageVO> messages = conversationMessageMapper.selectList(Wrappers.<LexicalRagConversationMessageEntity>lambdaQuery()
                        .eq(LexicalRagConversationMessageEntity::getConversationSessionId, session.getId())
                        .orderByAsc(LexicalRagConversationMessageEntity::getCreatedAt)
                        .orderByAsc(LexicalRagConversationMessageEntity::getId))
                .stream()
                .map(this::toMessageView)
                .toList();
        return new LexicalRagConversationDetailVO(
                session.getConversationId(),
                session.getTitle(),
                session.getScene(),
                session.getLastMessageAt(),
                messages
        );
    }

    @Transactional
    public LexicalRagConversationSessionEntity getOrCreateConversation(Long studentUserId, String conversationId, String firstQuery) {
        if (conversationId != null && !conversationId.isBlank()) {
            return requireConversation(studentUserId, conversationId);
        }
        LexicalRagConversationSessionEntity entity = new LexicalRagConversationSessionEntity();
        entity.setConversationId(UUID.randomUUID().toString());
        entity.setScene(AiConstants.SCENE_LEXICAL_RAG_QUERY);
        entity.setStudentUserId(studentUserId);
        entity.setTitle(buildTitle(firstQuery));
        entity.setLastMessageAt(LocalDateTime.now());
        conversationSessionMapper.insert(entity);
        return entity;
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> recentChatHistory(Long conversationSessionId) {
        List<LexicalRagConversationMessageEntity> newestFirst = conversationMessageMapper.selectList(Wrappers.<LexicalRagConversationMessageEntity>lambdaQuery()
                .eq(LexicalRagConversationMessageEntity::getConversationSessionId, conversationSessionId)
                .orderByDesc(LexicalRagConversationMessageEntity::getCreatedAt)
                .orderByDesc(LexicalRagConversationMessageEntity::getId)
                .last("LIMIT " + DEFAULT_CONTEXT_MESSAGE_LIMIT));
        List<LexicalRagConversationMessageEntity> oldestFirst = new ArrayList<>(newestFirst);
        Collections.reverse(oldestFirst);
        return oldestFirst.stream()
                .map(entity -> new ChatMessage(entity.getRole(), entity.getContentText()))
                .toList();
    }

    @Transactional
    public void saveUserMessage(LexicalRagConversationSessionEntity session, String query) {
        LexicalRagConversationMessageEntity entity = new LexicalRagConversationMessageEntity();
        entity.setConversationSessionId(session.getId());
        entity.setRole("user");
        entity.setContentText(query);
        conversationMessageMapper.insert(entity);
        touchConversation(session, LocalDateTime.now());
    }

    @Transactional
    public void saveAssistantMessage(LexicalRagConversationSessionEntity session, LexicalRagAnswerVO response) {
        LexicalRagConversationMessageEntity entity = new LexicalRagConversationMessageEntity();
        entity.setConversationSessionId(session.getId());
        entity.setRole("assistant");
        entity.setContentText(buildAssistantContentText(response));
        entity.setPayloadJson(aiJsonCodec.write(response));
        entity.setRequestId(response.requestId());
        entity.setGenerationSource(response.generationSource());
        entity.setModel(response.model());
        entity.setGrounded(response.grounded());
        entity.setFallbackReason(response.fallbackReason());
        conversationMessageMapper.insert(entity);
        touchConversation(session, LocalDateTime.now());
    }

    private void touchConversation(LexicalRagConversationSessionEntity session, LocalDateTime lastMessageAt) {
        session.setLastMessageAt(lastMessageAt);
        conversationSessionMapper.updateById(session);
    }

    private String buildAssistantContentText(LexicalRagAnswerVO response) {
        StringBuilder builder = new StringBuilder(response.answer().trim());
        if (response.explanation() != null && !response.explanation().isBlank()) {
            builder.append("\n\nExplanation:\n").append(response.explanation().trim());
        }
        if (response.recommendedActions() != null && !response.recommendedActions().isEmpty()) {
            builder.append("\n\nRecommended actions:");
            for (String action : response.recommendedActions()) {
                if (action == null || action.isBlank()) {
                    continue;
                }
                builder.append("\n- ").append(action.trim());
            }
        }
        return builder.toString();
    }

    private LexicalRagConversationSessionEntity requireConversation(Long studentUserId, String conversationId) {
        LexicalRagConversationSessionEntity entity = conversationSessionMapper.selectOne(Wrappers.<LexicalRagConversationSessionEntity>lambdaQuery()
                .eq(LexicalRagConversationSessionEntity::getConversationId, conversationId)
                .eq(LexicalRagConversationSessionEntity::getStudentUserId, studentUserId)
                .eq(LexicalRagConversationSessionEntity::getScene, AiConstants.SCENE_LEXICAL_RAG_QUERY)
                .last("LIMIT 1"));
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Conversation not found", 404);
        }
        return entity;
    }

    private LexicalRagConversationMessageVO toMessageView(LexicalRagConversationMessageEntity entity) {
        return new LexicalRagConversationMessageVO(
                entity.getId(),
                entity.getRole(),
                entity.getContentText(),
                aiJsonCodec.read(entity.getPayloadJson(), LexicalRagAnswerVO.class),
                entity.getRequestId(),
                entity.getCreatedAt()
        );
    }

    private String buildTitle(String firstQuery) {
        String normalized = firstQuery == null ? "" : firstQuery.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return "New conversation";
        }
        return normalized.length() <= 32 ? normalized : normalized.substring(0, 32);
    }
}
