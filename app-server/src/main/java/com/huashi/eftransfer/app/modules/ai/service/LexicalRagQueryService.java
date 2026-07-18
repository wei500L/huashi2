package com.huashi.eftransfer.app.modules.ai.service;

import com.huashi.eftransfer.app.common.config.AiGatewayClientProperties;
import com.huashi.eftransfer.app.common.util.SecurityUtils;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayCallResult;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayClient;
import com.huashi.eftransfer.app.integration.ai.client.AiGatewayFailureReason;
import com.huashi.eftransfer.app.modules.ai.dto.LexicalRagConversationPageQuery;
import com.huashi.eftransfer.app.modules.ai.dto.LexicalRagQueryRequest;
import com.huashi.eftransfer.app.modules.ai.entity.AiGenerationRecordEntity;
import com.huashi.eftransfer.app.modules.ai.entity.LexicalRagConversationSessionEntity;
import com.huashi.eftransfer.app.modules.ai.support.AiConstants;
import com.huashi.eftransfer.app.modules.ai.support.AiJsonCodec;
import com.huashi.eftransfer.app.modules.ai.support.AiUsageSummary;
import com.huashi.eftransfer.app.modules.ai.support.LexicalStructuredAnswerPayload;
import com.huashi.eftransfer.app.modules.ai.vo.LexicalRagAnswerVO;
import com.huashi.eftransfer.app.modules.ai.vo.LexicalRagConversationDetailVO;
import com.huashi.eftransfer.app.modules.ai.vo.LexicalRagConversationSummaryVO;
import com.huashi.eftransfer.shared.ai.ChatMessage;
import com.huashi.eftransfer.shared.ai.RagCitation;
import com.huashi.eftransfer.shared.ai.RagContextChunk;
import com.huashi.eftransfer.shared.ai.RagRetrieveRequest;
import com.huashi.eftransfer.shared.ai.RagRetrieveResponse;
import com.huashi.eftransfer.shared.ai.StructuredChatRequest;
import com.huashi.eftransfer.shared.ai.StructuredChatResponse;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.exception.BusinessException;
import com.huashi.eftransfer.shared.page.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class LexicalRagQueryService {

    private static final List<String> SOURCE_TYPES = List.of("LEXICAL_PAIR", "LEXICAL_SENSE", "LEXICAL_EXAMPLE");
    private static final String PROMPT_FOLDER = "lexical-rag-query";
    private static final int MAX_PROMPT_CONTEXT_CHARS = 72_000;
    private static final int MAX_HISTORY_MESSAGE_CHARS = 8_000;
    private static final String SCHEMA_NAME = "LexicalRagAnswer";

    private final AiGatewayClient aiGatewayClient;
    private final AiPromptTemplateService aiPromptTemplateService;
    private final AiOutputSchemaFactory aiOutputSchemaFactory;
    private final AiResponseValidator aiResponseValidator;
    private final AiGenerationRecordService aiGenerationRecordService;
    private final LexicalRagConversationService lexicalRagConversationService;
    private final AiJsonCodec aiJsonCodec;
    private final AiGatewayClientProperties aiGatewayClientProperties;

    public LexicalRagQueryService(
            AiGatewayClient aiGatewayClient,
            AiPromptTemplateService aiPromptTemplateService,
            AiOutputSchemaFactory aiOutputSchemaFactory,
            AiResponseValidator aiResponseValidator,
            AiGenerationRecordService aiGenerationRecordService,
            LexicalRagConversationService lexicalRagConversationService,
            AiJsonCodec aiJsonCodec,
            AiGatewayClientProperties aiGatewayClientProperties
    ) {
        this.aiGatewayClient = aiGatewayClient;
        this.aiPromptTemplateService = aiPromptTemplateService;
        this.aiOutputSchemaFactory = aiOutputSchemaFactory;
        this.aiResponseValidator = aiResponseValidator;
        this.aiGenerationRecordService = aiGenerationRecordService;
        this.lexicalRagConversationService = lexicalRagConversationService;
        this.aiJsonCodec = aiJsonCodec;
        this.aiGatewayClientProperties = aiGatewayClientProperties;
    }

    @Transactional(readOnly = true)
    public PageResult<LexicalRagConversationSummaryVO> pageConversations(LexicalRagConversationPageQuery query) {
        return lexicalRagConversationService.pageMine(currentUserId(), query);
    }

    @Transactional(readOnly = true)
    public LexicalRagConversationDetailVO getConversationDetail(String conversationId) {
        return lexicalRagConversationService.detail(currentUserId(), conversationId);
    }

    @Transactional
    public LexicalRagAnswerVO query(LexicalRagQueryRequest request) {
        long startedAt = System.nanoTime();
        String requestId = UUID.randomUUID().toString();
        String promptVersion = AiConstants.DEFAULT_PROMPT_VERSION;
        Long studentUserId = currentUserId();
        LexicalRagConversationSessionEntity conversation = lexicalRagConversationService.getOrCreateConversation(
                studentUserId,
                request.conversationId(),
                request.query()
        );
        List<ChatMessage> messageHistory = lexicalRagConversationService.recentChatHistory(conversation.getId());
        lexicalRagConversationService.saveUserMessage(conversation, request.query());

        Map<String, Object> inputPayload = new LinkedHashMap<>();
        inputPayload.put("query", request.query());
        inputPayload.put("conversationId", conversation.getConversationId());
        inputPayload.put("sourceTypes", SOURCE_TYPES);
        inputPayload.put("messageHistory", messageHistory);

        Map<String, Object> rawResponses = new LinkedHashMap<>();
        AiUsageSummary usageSummary = new AiUsageSummary();

        AiGatewayCallResult<RagRetrieveResponse> retrieveResult = aiGatewayClient.ragRetrieve(new RagRetrieveRequest(
                request.query(),
                SOURCE_TYPES,
                List.of(),
                conversation.getConversationId(),
                messageHistory
        ));
        rawResponses.put("ragRetrieve", retrieveResult);

        if (!retrieveResult.success()) {
            LexicalRagAnswerVO fallback = buildFallbackWithoutContext(
                    requestId,
                    conversation.getConversationId(),
                    request.query(),
                    retrieveResult.failureReason(),
                    elapsedMillis(startedAt)
            );
            return finalizeResponse(fallback, conversation, studentUserId, promptVersion, null, null, usageSummary, inputPayload, rawResponses);
        }

        RagRetrieveResponse retrieved = retrieveResult.data();
        if (retrieved == null || !retrieved.grounded() || retrieved.citations() == null || retrieved.citations().isEmpty()) {
            LexicalRagAnswerVO fallback = buildFallbackWithoutContext(
                    requestId,
                    conversation.getConversationId(),
                    request.query(),
                    AiGatewayFailureReason.NO_GROUNDED_CONTEXT,
                    elapsedMillis(startedAt)
            );
            return finalizeResponse(fallback, conversation, studentUserId, promptVersion, null, null, usageSummary, inputPayload, rawResponses);
        }

        StructuredChatRequest structuredRequest = new StructuredChatRequest(
                buildStructuredMessages(messageHistory, request.query(), retrieved, promptVersion),
                null,
                0.2d,
                SCHEMA_NAME,
                Boolean.TRUE,
                aiOutputSchemaFactory.lexicalRagAnswerSchema()
        );

        AiGatewayCallResult<StructuredChatResponse> structuredResult = aiGatewayClient.structuredChat(structuredRequest);
        rawResponses.put("structuredChat", structuredResult);

        if (!structuredResult.success()) {
            LexicalRagAnswerVO fallback = buildFallbackWithRetrievedContext(
                    requestId,
                    conversation.getConversationId(),
                    request.query(),
                    retrieved,
                    structuredResult.failureReason(),
                    elapsedMillis(startedAt)
            );
            return finalizeResponse(fallback, conversation, studentUserId, promptVersion, null, null, usageSummary, inputPayload, rawResponses);
        }

        usageSummary.addStructured(structuredResult.data());
        if (structuredResult.data().structuredData() == null || structuredResult.data().structuredData().isEmpty()) {
            rawResponses.put("validationError", "Structured lexical RAG payload was empty");
            LexicalRagAnswerVO fallback = buildFallbackWithRetrievedContext(
                    requestId,
                    conversation.getConversationId(),
                    request.query(),
                    retrieved,
                    AiGatewayFailureReason.INVALID_JSON,
                    elapsedMillis(startedAt)
            );
            return finalizeResponse(fallback, conversation, studentUserId, promptVersion, null, null, usageSummary, inputPayload, rawResponses);
        }

        try {
            Set<String> availableCitationIds = retrieved.citations().stream()
                    .map(RagCitation::citationId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            LexicalStructuredAnswerPayload payload = aiResponseValidator.validateLexicalRagAnswer(
                    structuredResult.data().structuredData(),
                    availableCitationIds
            );
            List<RagCitation> citations = selectCitations(payload.citationIds(), retrieved.citations());
            List<RagContextChunk> contextChunks = selectContextChunks(payload.citationIds(), retrieved.contextChunks());
            LexicalRagAnswerVO response = new LexicalRagAnswerVO(
                    requestId,
                    conversation.getConversationId(),
                    AiConstants.GENERATION_SOURCE_AI,
                    structuredResult.data().model(),
                    elapsedMillis(startedAt),
                    true,
                    payload.answer(),
                    payload.explanation(),
                    payload.recommendedActions(),
                    payload.confidence(),
                    payload.citationIds(),
                    citations,
                    contextChunks,
                    null
            );
            return finalizeResponse(
                    response,
                    conversation,
                    studentUserId,
                    promptVersion,
                    structuredResult.data().model(),
                    structuredResult.data().providerRequestId(),
                    usageSummary,
                    inputPayload,
                    rawResponses
            );
        } catch (IllegalStateException validationException) {
            rawResponses.put("validationError", validationException.getMessage());
            LexicalRagAnswerVO fallback = buildFallbackWithRetrievedContext(
                    requestId,
                    conversation.getConversationId(),
                    request.query(),
                    retrieved,
                    AiGatewayFailureReason.SCHEMA_VALIDATION_FAILED,
                    elapsedMillis(startedAt)
            );
            return finalizeResponse(fallback, conversation, studentUserId, promptVersion, null, null, usageSummary, inputPayload, rawResponses);
        }
    }

    private LexicalRagAnswerVO finalizeResponse(
            LexicalRagAnswerVO response,
            LexicalRagConversationSessionEntity conversation,
            Long studentUserId,
            String promptVersion,
            String model,
            String providerRequestId,
            AiUsageSummary usageSummary,
            Map<String, Object> inputPayload,
            Map<String, Object> rawResponses
    ) {
        persistGenerationRecord(
                response,
                studentUserId,
                promptVersion,
                model,
                providerRequestId,
                usageSummary,
                inputPayload,
                rawResponses
        );
        lexicalRagConversationService.saveAssistantMessage(conversation, response);
        return response;
    }

    private List<ChatMessage> buildStructuredMessages(
            List<ChatMessage> messageHistory,
            String query,
            RagRetrieveResponse retrieved,
            String promptVersion
    ) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", aiPromptTemplateService.loadSystemPrompt(PROMPT_FOLDER, promptVersion)));
        List<ChatMessage> boundedHistory = messageHistory.stream()
                .filter(java.util.Objects::nonNull)
                .filter(message -> !"system".equals(message.role()))
                .filter(message -> message.content() != null && !message.content().isBlank())
                .map(message -> new ChatMessage(
                        message.role(),
                        boundedText(message.content(), MAX_HISTORY_MESSAGE_CHARS)
                ))
                .toList();
        messages.add(new ChatMessage("user", aiPromptTemplateService.renderUserPrompt(
                PROMPT_FOLDER,
                promptVersion,
                Map.of(
                        "QUERY", query,
                        "HISTORY_JSON", aiJsonCodec.write(boundedHistory),
                        "CONTEXT_JSON", aiJsonCodec.write(promptPayload(query, retrieved))
                )
        )));
        return messages;
    }

    private Map<String, Object> promptPayload(String query, RagRetrieveResponse retrieved) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("query", query);
        payload.put("grounded", retrieved.grounded());
        payload.put("uncertaintyNote", retrieved.uncertaintyNote());
        payload.put("citations", retrieved.citations());
        payload.put("contextChunks", boundedContextChunks(retrieved.contextChunks()));
        return payload;
    }

    private List<RagContextChunk> boundedContextChunks(List<RagContextChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        List<RagContextChunk> bounded = new ArrayList<>(chunks.size());
        int remainingContextChars = MAX_PROMPT_CONTEXT_CHARS;
        for (int index = 0; index < chunks.size(); index++) {
            RagContextChunk chunk = chunks.get(index);
            int remainingChunks = chunks.size() - index;
            String content = boundedText(chunk.content(), Math.max(1, remainingContextChars / remainingChunks));
            bounded.add(new RagContextChunk(
                    chunk.citationId(),
                    chunk.sourceType(),
                    chunk.sourceId(),
                    chunk.title(),
                    content,
                    chunk.snippet(),
                    chunk.score(),
                    Map.of()
            ));
            remainingContextChars = Math.max(0, remainingContextChars - content.length());
        }
        return List.copyOf(bounded);
    }

    private String boundedText(String value, int maxChars) {
        if (value == null || value.isEmpty() || maxChars <= 0) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        if (maxChars <= 16) {
            return value.substring(0, maxChars);
        }
        return value.substring(0, maxChars - 16) + "\n...[truncated]";
    }

    private List<RagCitation> selectCitations(List<String> citationIds, List<RagCitation> citations) {
        Map<String, RagCitation> citationMap = citations.stream()
                .collect(java.util.stream.Collectors.toMap(RagCitation::citationId, citation -> citation, (left, right) -> left, LinkedHashMap::new));
        return citationIds.stream()
                .map(citationMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<RagContextChunk> selectContextChunks(List<String> citationIds, List<RagContextChunk> contextChunks) {
        Map<String, RagContextChunk> contextMap = contextChunks.stream()
                .collect(java.util.stream.Collectors.toMap(RagContextChunk::citationId, chunk -> chunk, (left, right) -> left, LinkedHashMap::new));
        return citationIds.stream()
                .map(contextMap::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private LexicalRagAnswerVO buildFallbackWithoutContext(
            String requestId,
            String conversationId,
            String query,
            AiGatewayFailureReason failureReason,
            long latencyMs
    ) {
        ensureFallbackAllowed(requestId);
        String normalizedQuery = query == null ? "" : query.toLowerCase(Locale.ROOT);
        List<String> actions = new ArrayList<>();
        String answer;
        String explanation;
        if (containsAny(normalizedQuery, "区别", "差别", "different", "difference")) {
            answer = "当前没有检索到可用词对证据，我先按规则给出辨析框架：先分开看词形、核心义项和典型语境，再判断它们是否属于容易触发负迁移的近形词。";
            explanation = "处理词对差异问题时，优先确认每个词的核心义项边界，再对比例句中的搭配和语境限制，这样能减少只凭表层词形作答。";
            actions.add("先分别写出两个词的核心义项和中文释义。");
            actions.add("再各造一个最典型例句，检查是否可以互换。");
            actions.add("最后把容易混淆的触发条件单独记录下来。");
        } else if (containsAny(normalizedQuery, "为什么", "原因", "why", "reason")) {
            answer = "当前没有检索到可用词对证据，我先按规则解释：这类混淆通常来自词形相似、已有母语或英语语义迁移，以及对语境线索利用不足。";
            explanation = "当学习者先看到熟悉词形时，往往会过早套用已有语义；如果没有继续检查上下文，就容易出现 false friend 或语境错配。";
            actions.add("先标出导致误判的表层相似点。");
            actions.add("再补一组能打破直觉的对比例句。");
            actions.add("用一句话复述正确判断依据，而不是只记答案。");
        } else {
            answer = "当前没有检索到可用词对证据，我先按规则回答：遇到词对问题时，先确认核心义项，再检查语境和固定搭配，最后做最小对比练习。";
            explanation = "没有可靠检索证据时，最稳妥的做法是回到辨义流程本身，避免模型在缺少上下文时编造词义或引用。";
            actions.add("先确认词对的基本释义。");
            actions.add("再检查典型语境和固定搭配。");
            actions.add("最后做一轮最小对比训练。");
        }

        return new LexicalRagAnswerVO(
                requestId,
                conversationId,
                AiConstants.GENERATION_SOURCE_RULE_FALLBACK,
                null,
                latencyMs,
                false,
                answer,
                explanation,
                actions,
                0.32d,
                List.of(),
                List.of(),
                List.of(),
                failureReason == null ? AiGatewayFailureReason.UNKNOWN.name() : failureReason.name()
        );
    }

    private LexicalRagAnswerVO buildFallbackWithRetrievedContext(
            String requestId,
            String conversationId,
            String query,
            RagRetrieveResponse retrieved,
            AiGatewayFailureReason failureReason,
            long latencyMs
    ) {
        ensureFallbackAllowed(requestId);
        List<RagCitation> citations = retrieved.citations() == null ? List.of() : retrieved.citations();
        List<RagContextChunk> contextChunks = retrieved.contextChunks() == null ? List.of() : retrieved.contextChunks();
        String inlineMarkers = citations.stream()
                .map(citation -> "[" + citation.citationId() + "]")
                .collect(java.util.stream.Collectors.joining(" "));
        RagCitation primaryCitation = citations.isEmpty() ? null : citations.get(0);
        RagContextChunk primaryChunk = contextChunks.isEmpty() ? null : contextChunks.get(0);
        String snippet = primaryChunk == null || primaryChunk.snippet() == null || primaryChunk.snippet().isBlank()
                ? (primaryCitation == null ? "暂无可直接引用的片段" : primaryCitation.snippet())
                : primaryChunk.snippet();
        String title = primaryCitation == null ? "词对知识片段" : primaryCitation.title();
        String answer = "基于已检索到的词对知识，当前最可靠的线索集中在 “%s” 上：%s %s".formatted(
                title,
                snippet == null ? "暂无摘要" : snippet,
                inlineMarkers
        ).trim();
        String explanation = buildRetrievedExplanation(query, title, inlineMarkers);
        List<String> actions = List.of(
                "先围绕当前检索到的词对片段做最小对比辨析。",
                "把检索片段中的核心义项或例句改写成自己的判断规则。",
                "再回到原问题，用语境而不是词形重新解释一次。"
        );
        List<String> citationIds = citations.stream().map(RagCitation::citationId).toList();
        return new LexicalRagAnswerVO(
                requestId,
                conversationId,
                AiConstants.GENERATION_SOURCE_RULE_FALLBACK,
                null,
                latencyMs,
                true,
                answer,
                explanation,
                actions,
                0.46d,
                citationIds,
                citations,
                contextChunks,
                failureReason == null ? AiGatewayFailureReason.UNKNOWN.name() : failureReason.name()
        );
    }

    private String buildRetrievedExplanation(String query, String title, String inlineMarkers) {
        String normalizedQuery = query == null ? "" : query.toLowerCase(Locale.ROOT);
        if (containsAny(normalizedQuery, "区别", "差别", "different", "difference")) {
            return "这次先依据已召回的片段说明 %s 的差异，再补充义项边界和语境限制，避免只按表层词形判断。%s"
                    .formatted(title, inlineMarkers)
                    .trim();
        }
        if (containsAny(normalizedQuery, "为什么", "原因", "why", "reason")) {
            return "这次先依据已召回的片段解释 %s 为什么容易触发负迁移，再把原因落到具体语境和判断依据上。%s"
                    .formatted(title, inlineMarkers)
                    .trim();
        }
        return "这次先依据已召回的片段回答你的问题，再把结论收束到最稳的辨义步骤上。%s".formatted(inlineMarkers).trim();
    }

    private boolean containsAny(String value, String... patterns) {
        for (String pattern : patterns) {
            if (value.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private void persistGenerationRecord(
            LexicalRagAnswerVO response,
            Long studentUserId,
            String promptVersion,
            String model,
            String providerRequestId,
            AiUsageSummary usageSummary,
            Map<String, Object> inputPayload,
            Map<String, Object> rawResponses
    ) {
        AiGenerationRecordEntity entity = new AiGenerationRecordEntity();
        entity.setRequestId(response.requestId());
        entity.setScene(AiConstants.SCENE_LEXICAL_RAG_QUERY);
        entity.setStudentUserId(studentUserId);
        entity.setPromptVersion(promptVersion);
        entity.setModel(model);
        entity.setProviderRequestId(providerRequestId);
        entity.setLatencyMs(response.latencyMs());
        Map<String, Object> tokenUsage = new LinkedHashMap<>();
        tokenUsage.put("promptTokens", usageSummary.promptTokens());
        tokenUsage.put("completionTokens", usageSummary.completionTokens());
        tokenUsage.put("rerankTokens", usageSummary.rerankTokens());
        tokenUsage.put("totalTokens", usageSummary.totalTokens());
        entity.setTokenUsageJson(aiJsonCodec.write(tokenUsage));
        entity.setInputPayloadJson(aiJsonCodec.write(inputPayload));
        entity.setRawResponseJson(aiJsonCodec.write(rawResponses));
        entity.setValidatedOutputJson(aiJsonCodec.write(response));
        entity.setGenerationSource(response.generationSource());
        entity.setFallbackReason(response.fallbackReason());
        entity.setGeneratedAt(LocalDateTime.now());
        aiGenerationRecordService.save(entity);
    }

    private void ensureFallbackAllowed(String requestId) {
        if (!aiGatewayClientProperties.isDegradeEnabled()) {
            throw new BusinessException(
                    ResultCode.AI_PROVIDER_UNAVAILABLE,
                    "AI generation failed for lexical RAG request " + requestId,
                    503
            );
        }
    }

    private Long currentUserId() {
        return SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BusinessException(ResultCode.UNAUTHORIZED, "Authentication required", 401));
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
