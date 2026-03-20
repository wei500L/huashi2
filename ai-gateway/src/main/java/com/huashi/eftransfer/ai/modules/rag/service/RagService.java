package com.huashi.eftransfer.ai.modules.rag.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huashi.eftransfer.ai.modules.rag.config.RagProperties;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeSourceTypes;
import com.huashi.eftransfer.ai.modules.rag.support.RagRetrievalResult;
import com.huashi.eftransfer.ai.modules.rag.support.RagSearchFilter;
import com.huashi.eftransfer.ai.modules.rag.vector.RagAdvisorVectorStore;
import com.huashi.eftransfer.shared.ai.RagAnswerRequest;
import com.huashi.eftransfer.shared.ai.RagAnswerResponse;
import com.huashi.eftransfer.shared.ai.RagCitation;
import com.huashi.eftransfer.shared.ai.RagContextChunk;
import com.huashi.eftransfer.shared.ai.RagExplainRiskRequest;
import com.huashi.eftransfer.shared.ai.RagExplainRiskResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RagService {

    private static final String ANSWER_SYSTEM_PROMPT = """
            You are an internal retrieval-augmented assistant for English-French lexical transfer training.
            Use the retrieved knowledge as the primary source of truth.
            When a claim is supported, cite it inline with the citation label such as [C1].
            If the evidence is insufficient or ambiguous, state that clearly.
            Do not invent facts, sources, or certainty.
            """;

    private static final String EXPLAIN_RISK_SYSTEM_PROMPT = """
            You are an internal retrieval-augmented assistant for lexical transfer risk analysis.
            Use the retrieved knowledge as the primary source of truth.
            Return JSON only with keys: riskExplanation, negativeTransferReason, priorityTrainingFocus, uncertaintyNote.
            Each field must be a string.
            Cite supported claims inline with citation labels such as [C1].
            If the evidence is insufficient, say so in uncertaintyNote.
            Do not invent sources or unsupported claims.
            """;

    private final ChatClient chatClient;
    private final RagRetrievalCapture ragRetrievalCapture;
    private final KnowledgeSearchService knowledgeSearchService;
    private final RagProperties ragProperties;
    private final ObjectMapper objectMapper;

    public RagService(
            ChatClient qwenChatClient,
            RagRetrievalCapture ragRetrievalCapture,
            KnowledgeSearchService knowledgeSearchService,
            RagProperties ragProperties,
            ObjectMapper objectMapper
    ) {
        this.chatClient = qwenChatClient;
        this.ragRetrievalCapture = ragRetrievalCapture;
        this.knowledgeSearchService = knowledgeSearchService;
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
    }

    public RagAnswerResponse answer(RagAnswerRequest request) {
        RagSearchFilter filter = new RagSearchFilter(normalizeSourceTypes(request.sourceTypes()), normalizeIds(request.sourceIds()));
        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(
                        new RagAdvisorVectorStore(knowledgeSearchService, ragRetrievalCapture, filter, ragProperties))
                .searchRequest(SearchRequest.builder()
                        .topK(ragProperties.getRetrieval().getFinalTopK())
                        .similarityThreshold(ragProperties.getRetrieval().getRecallThreshold())
                        .build())
                .build();

        String answer = chatClient.prompt()
                .system(ANSWER_SYSTEM_PROMPT)
                .advisors(advisor)
                .user(request.query())
                .call()
                .content();

        RagRetrievalResult retrievalResult = consumeResult(request.query());
        boolean grounded = !retrievalResult.chunks().isEmpty();
        String uncertaintyNote = grounded ? null : "No sufficiently relevant knowledge chunks were retrieved from the knowledge base.";

        return new RagAnswerResponse(
                answer,
                grounded,
                uncertaintyNote,
                toCitations(retrievalResult),
                toContextChunks(retrievalResult)
        );
    }

    public RagExplainRiskResponse explainRisk(RagExplainRiskRequest request) {
        RagSearchFilter filter = new RagSearchFilter(
                Set.of(
                        KnowledgeSourceTypes.LEXICAL_PAIR,
                        KnowledgeSourceTypes.LEXICAL_SENSE,
                        KnowledgeSourceTypes.LEXICAL_EXAMPLE,
                        KnowledgeSourceTypes.ERROR_TYPE,
                        KnowledgeSourceTypes.INTERVENTION_TEMPLATE,
                        KnowledgeSourceTypes.TRAINING_GUIDE,
                        KnowledgeSourceTypes.COURSE_GUIDE
                ),
                Set.of()
        );
        String prompt = buildExplainRiskPrompt(request);
        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(
                        new RagAdvisorVectorStore(knowledgeSearchService, ragRetrievalCapture, filter, ragProperties))
                .searchRequest(SearchRequest.builder()
                        .topK(ragProperties.getRetrieval().getFinalTopK())
                        .similarityThreshold(ragProperties.getRetrieval().getRecallThreshold())
                        .build())
                .build();

        String content = chatClient.prompt()
                .system(EXPLAIN_RISK_SYSTEM_PROMPT)
                .advisors(advisor)
                .user(prompt)
                .call()
                .content();

        RagRetrievalResult retrievalResult = consumeResult(prompt);
        ExplainRiskPayload payload = parseExplainRiskPayload(content, retrievalResult.chunks().isEmpty());

        return new RagExplainRiskResponse(
                payload.riskExplanation(),
                payload.negativeTransferReason(),
                payload.priorityTrainingFocus(),
                payload.uncertaintyNote(),
                toCitations(retrievalResult),
                toContextChunks(retrievalResult)
        );
    }

    private RagRetrievalResult consumeResult(String query) {
        RagRetrievalResult retrievalResult = ragRetrievalCapture.consume();
        return retrievalResult == null ? RagRetrievalResult.empty(query) : retrievalResult;
    }

    private List<RagCitation> toCitations(RagRetrievalResult retrievalResult) {
        return retrievalResult.chunks().stream()
                .map(chunk -> new RagCitation(
                        chunk.citationId(),
                        chunk.sourceType(),
                        chunk.sourceId(),
                        chunk.title(),
                        chunk.snippet(),
                        chunk.score()
                ))
                .toList();
    }

    private List<RagContextChunk> toContextChunks(RagRetrievalResult retrievalResult) {
        return retrievalResult.chunks().stream()
                .map(chunk -> new RagContextChunk(
                        chunk.citationId(),
                        chunk.sourceType(),
                        chunk.sourceId(),
                        chunk.title(),
                        chunk.content(),
                        chunk.snippet(),
                        chunk.score(),
                        chunk.metadata()
                ))
                .toList();
    }

    private Set<String> normalizeSourceTypes(List<String> sourceTypes) {
        if (sourceTypes == null || sourceTypes.isEmpty()) {
            return Set.of();
        }
        return sourceTypes.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> normalizeIds(List<String> sourceIds) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Set.of();
        }
        return sourceIds.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String buildExplainRiskPrompt(RagExplainRiskRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("Diagnostic summary:\n");
        if (request.diagnosticSummary() != null) {
            builder.append("- negativeTransferRisk: ").append(request.diagnosticSummary().negativeTransferRisk()).append('\n');
            builder.append("- contextSensitivity: ").append(request.diagnosticSummary().contextSensitivity()).append('\n');
            builder.append("- overallAccuracy: ").append(request.diagnosticSummary().overallAccuracy()).append('\n');
            builder.append("- averageReactionTimeMs: ").append(request.diagnosticSummary().averageReactionTimeMs()).append('\n');
        }
        builder.append("Error type distribution:\n");
        if (request.errorTypeDistribution() != null) {
            request.errorTypeDistribution().forEach(stat -> builder
                    .append("- ")
                    .append(stat.code())
                    .append(" count=")
                    .append(stat.count())
                    .append(" ratio=")
                    .append(stat.ratio())
                    .append('\n'));
        }
        builder.append("High risk lexical pairs:\n");
        request.highRiskLexicalPairs().forEach(pair -> builder
                .append("- pairId=")
                .append(pair.lexicalPairId())
                .append(", english=")
                .append(pair.englishWord())
                .append(", french=")
                .append(pair.frenchWord())
                .append(", lexicalPairType=")
                .append(pair.lexicalPairType())
                .append(", riskScore=")
                .append(pair.riskScore())
                .append(", dominantErrorType=")
                .append(pair.dominantErrorType())
                .append(", riskLevel=")
                .append(pair.riskLevel())
                .append('\n'));
        builder.append("""
                Explain the learner's transfer risk, why negative transfer is likely, and what should be trained first.
                Use concise evidence-based wording and cite supporting knowledge as [C1], [C2], etc.
                """);
        return builder.toString();
    }

    private ExplainRiskPayload parseExplainRiskPayload(String content, boolean emptyEvidence) {
        String sanitized = content == null ? "" : content.trim();
        if (sanitized.startsWith("```")) {
            sanitized = sanitized.replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "");
        }
        try {
            return objectMapper.readValue(sanitized, ExplainRiskPayload.class);
        } catch (JsonProcessingException ex) {
            return new ExplainRiskPayload(
                    sanitized.isBlank() ? "Model did not return a structured risk explanation." : sanitized,
                    "The model response could not be parsed as structured JSON.",
                    "Prioritize the highest-risk lexical pairs first.",
                    emptyEvidence
                            ? "No sufficiently relevant knowledge chunks were retrieved from the knowledge base."
                            : "The model response format was invalid, so part of the explanation may be unreliable."
            );
        }
    }

    private record ExplainRiskPayload(
            String riskExplanation,
            String negativeTransferReason,
            String priorityTrainingFocus,
            String uncertaintyNote
    ) {
    }
}
