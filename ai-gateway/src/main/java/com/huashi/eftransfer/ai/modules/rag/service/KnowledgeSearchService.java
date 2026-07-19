package com.huashi.eftransfer.ai.modules.rag.service;

import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.ai.integration.provider.AiProviderRegistry;
import com.huashi.eftransfer.ai.modules.rag.repository.KnowledgeStoreRepository;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeSearchCandidate;
import com.huashi.eftransfer.ai.modules.rag.support.EmbeddingTextSupport;
import com.huashi.eftransfer.ai.modules.rag.support.RagRetrievedChunk;
import com.huashi.eftransfer.ai.modules.rag.support.RagRetrievalResult;
import com.huashi.eftransfer.ai.modules.rag.support.RagSearchFilter;
import com.huashi.eftransfer.ai.modules.rag.support.RetrievalQueryPlan;
import com.huashi.eftransfer.shared.ai.EmbeddingBatchRequest;
import com.huashi.eftransfer.shared.ai.EmbeddingResponse;
import com.huashi.eftransfer.shared.ai.RerankItem;
import com.huashi.eftransfer.shared.ai.RerankRequest;
import com.huashi.eftransfer.shared.ai.RerankResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);
    private static final int MAX_RERANK_DOCUMENT_CHARS = 120_000;
    private static final int RRF_RANK_CONSTANT = 60;
    private static final double VECTOR_ROUTE_WEIGHT = 1.0d;
    private static final double LEXICAL_ROUTE_WEIGHT = 1.35d;
    private static final int MAX_CHUNKS_PER_LEXICAL_PAIR = 3;

    private final AiProviderRegistry aiProviderRegistry;
    private final KnowledgeStoreRepository knowledgeStoreRepository;
    private final AiRuntimeConfigService runtimeConfigService;
    private final RetrievalQueryPlanner retrievalQueryPlanner;

    public KnowledgeSearchService(
            AiProviderRegistry aiProviderRegistry,
            KnowledgeStoreRepository knowledgeStoreRepository,
            AiRuntimeConfigService runtimeConfigService,
            RetrievalQueryPlanner retrievalQueryPlanner
    ) {
        this.aiProviderRegistry = aiProviderRegistry;
        this.knowledgeStoreRepository = knowledgeStoreRepository;
        this.runtimeConfigService = runtimeConfigService;
        this.retrievalQueryPlanner = retrievalQueryPlanner;
    }

    public RagRetrievalResult search(String query, RagSearchFilter filter) {
        return search(query, filter, null);
    }

    public RagRetrievalResult search(String query, RagSearchFilter filter, SearchRequest searchRequest) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("RAG search query must not be blank");
        }
        var retrieval = runtimeConfigService.current().config().rag().retrieval();
        int finalTopK = resolveFinalTopK(searchRequest, retrieval.finalTopK());
        double recallThreshold = resolveRecallThreshold(searchRequest, retrieval.recallThreshold());
        RetrievalQueryPlan queryPlan = retrievalQueryPlanner.plan(query);
        Map<Long, FusionAccumulator> fusedCandidates = new LinkedHashMap<>();

        addVectorCandidates(
                fusedCandidates,
                queryPlan,
                filter,
                retrieval.recallTopK(),
                retrieval.hnswEfSearch(),
                recallThreshold
        );
        addLexicalCandidates(fusedCandidates, queryPlan, filter, retrieval.recallTopK());

        List<KnowledgeSearchCandidate> recallCandidates = fusedCandidates.values().stream()
                .sorted(Comparator.comparingDouble(FusionAccumulator::fusionScore).reversed()
                        .thenComparing(accumulator -> accumulator.candidate().chunkId()))
                .limit(retrieval.recallTopK())
                .map(this::toFusedCandidate)
                .toList();

        if (recallCandidates.isEmpty()) {
            return RagRetrievalResult.empty(query);
        }

        List<String> rerankDocuments = recallCandidates.stream()
                .map(this::toRerankDocument)
                .toList();
        RerankResponse rerankResponse;
        try {
            rerankResponse = aiProviderRegistry.rerank(new RerankRequest(
                    null,
                    query,
                    rerankDocuments,
                    Math.min(retrieval.rerankTopN(), rerankDocuments.size()),
                    Boolean.TRUE,
                    null,
                    "Rank lexical transfer knowledge by relevance to the user query."
            ));
        } catch (RuntimeException exception) {
            log.warn("event=knowledge_search_rerank_failed candidateCount={} message={}",
                    recallCandidates.size(), exception.getMessage());
            return buildRetrievalResult(query, toRecallOrderedChunks(recallCandidates, finalTopK));
        }

        if (rerankResponse == null || rerankResponse.items() == null || rerankResponse.items().isEmpty()) {
            log.warn("event=knowledge_search_rerank_unavailable candidateCount={} reason=empty_response", recallCandidates.size());
            return buildRetrievalResult(query, toRecallOrderedChunks(recallCandidates, finalTopK));
        }

        Map<Integer, KnowledgeSearchCandidate> candidateByIndex = new LinkedHashMap<>();
        for (int index = 0; index < recallCandidates.size(); index++) {
            candidateByIndex.put(index, recallCandidates.get(index));
        }

        List<RagRetrievedChunk> finalChunks = new ArrayList<>();
        Map<String, Integer> lexicalPairChunkCounts = new HashMap<>();
        if (rerankResponse.items() != null) {
            int citationIndex = 1;
            for (RerankItem rerankItem : rerankResponse.items()) {
                KnowledgeSearchCandidate candidate = candidateByIndex.get(rerankItem.index());
                if (candidate == null) {
                    continue;
                }
                boolean exactLexicalMatch = Boolean.TRUE.equals(candidate.metadata().get("exactLexicalMatch"));
                if (rerankItem.relevanceScore() < retrieval.rerankThreshold() && !exactLexicalMatch) {
                    continue;
                }
                if (!withinLexicalPairDiversityLimit(candidate, lexicalPairChunkCounts)) {
                    continue;
                }
                String citationId = "C" + citationIndex++;
                finalChunks.add(new RagRetrievedChunk(
                        candidate.chunkId(),
                        citationId,
                        candidate.sourceType(),
                        candidate.sourceId(),
                        candidate.title(),
                        candidate.content(),
                        snippet(candidate.content()),
                        exactLexicalMatch
                                ? Math.max(0.99d, rerankItem.relevanceScore())
                                : rerankItem.relevanceScore(),
                        candidate.metadata()
                ));
                if (finalChunks.size() >= finalTopK) {
                    break;
                }
            }
        }

        return buildRetrievalResult(query, finalChunks);
    }

    private void addVectorCandidates(
            Map<Long, FusionAccumulator> fusedCandidates,
            RetrievalQueryPlan queryPlan,
            RagSearchFilter filter,
            int recallTopK,
            int hnswEfSearch,
            double recallThreshold
    ) {
        if (queryPlan.semanticQueries().isEmpty()) {
            return;
        }
        try {
            EmbeddingResponse embeddingResponse = aiProviderRegistry.embedBatch(new EmbeddingBatchRequest(
                    queryPlan.semanticQueries().stream()
                            .map(EmbeddingTextSupport::toRetrievalQuery)
                            .toList(),
                    null,
                    null,
                    null
            ));
            if (embeddingResponse.items() == null || embeddingResponse.items().isEmpty()) {
                return;
            }
            for (var embeddingItem : embeddingResponse.items()) {
                List<KnowledgeSearchCandidate> candidates = knowledgeStoreRepository.similaritySearch(
                                embeddingItem.embedding(),
                                embeddingResponse.model(),
                                filter,
                                recallTopK,
                                hnswEfSearch
                        ).stream()
                        .filter(candidate -> candidate.similarityScore() >= recallThreshold)
                        .toList();
                addRankedRoute(fusedCandidates, candidates, VECTOR_ROUTE_WEIGHT, "vector");
            }
        } catch (RuntimeException exception) {
            log.warn("event=knowledge_search_vector_failed queryCount={} message={}",
                    queryPlan.semanticQueries().size(), exception.getMessage());
        }
    }

    private void addLexicalCandidates(
            Map<Long, FusionAccumulator> fusedCandidates,
            RetrievalQueryPlan queryPlan,
            RagSearchFilter filter,
            int recallTopK
    ) {
        for (String term : queryPlan.lexicalTerms()) {
            try {
                List<KnowledgeSearchCandidate> candidates = knowledgeStoreRepository.lexicalSearch(term, filter, recallTopK);
                addRankedRoute(fusedCandidates, candidates, LEXICAL_ROUTE_WEIGHT, "lexical");
            } catch (RuntimeException exception) {
                log.warn("event=knowledge_search_lexical_failed term={} message={}", term, exception.getMessage());
            }
        }
    }

    private void addRankedRoute(
            Map<Long, FusionAccumulator> fusedCandidates,
            List<KnowledgeSearchCandidate> candidates,
            double routeWeight,
            String route
    ) {
        for (int index = 0; index < candidates.size(); index++) {
            KnowledgeSearchCandidate candidate = candidates.get(index);
            double rankContribution = routeWeight / (RRF_RANK_CONSTANT + index + 1.0d);
            double scoreContribution = routeWeight * candidate.similarityScore() * 0.01d;
            FusionAccumulator accumulator = fusedCandidates.computeIfAbsent(
                    candidate.chunkId(),
                    ignored -> new FusionAccumulator(candidate)
            );
            accumulator.add(rankContribution + scoreContribution, route, candidate.similarityScore());
        }
    }

    private KnowledgeSearchCandidate toFusedCandidate(FusionAccumulator accumulator) {
        KnowledgeSearchCandidate candidate = accumulator.candidate();
        Map<String, Object> metadata = new LinkedHashMap<>(candidate.metadata());
        metadata.put("retrievalSignals", List.copyOf(accumulator.routes()));
        metadata.put("retrievalFusionScore", accumulator.fusionScore());
        metadata.put("exactLexicalMatch", accumulator.exactLexicalMatch());
        return new KnowledgeSearchCandidate(
                candidate.chunkId(),
                candidate.sourceType(),
                candidate.sourceId(),
                candidate.title(),
                candidate.content(),
                metadata,
                accumulator.fusionScore()
        );
    }

    private boolean withinLexicalPairDiversityLimit(
            KnowledgeSearchCandidate candidate,
            Map<String, Integer> lexicalPairChunkCounts
    ) {
        Object lexicalPairId = candidate.metadata().get("lexicalPairId");
        if (lexicalPairId == null) {
            return true;
        }
        String key = String.valueOf(lexicalPairId);
        int count = lexicalPairChunkCounts.getOrDefault(key, 0);
        if (count >= MAX_CHUNKS_PER_LEXICAL_PAIR) {
            return false;
        }
        lexicalPairChunkCounts.put(key, count + 1);
        return true;
    }

    private String toRerankDocument(KnowledgeSearchCandidate candidate) {
        String document = """
                Title: %s
                Source Type: %s
                Source Id: %s
                Content:
                %s
                """.formatted(candidate.title(), candidate.sourceType(), candidate.sourceId(), candidate.content());
        if (document.length() <= MAX_RERANK_DOCUMENT_CHARS) {
            return document;
        }
        return document.substring(0, MAX_RERANK_DOCUMENT_CHARS - 16) + "\n...[truncated]";
    }

    private Document toDocument(RagRetrievedChunk chunk) {
        Map<String, Object> metadata = new LinkedHashMap<>(chunk.metadata());
        metadata.put("citationId", chunk.citationId());
        metadata.put("sourceType", chunk.sourceType());
        metadata.put("sourceId", chunk.sourceId());
        metadata.put("title", chunk.title());
        metadata.put("score", chunk.score());
        return new Document("""
                Citation: [%s]
                Title: %s
                Source Type: %s
                Source Id: %s
                Content:
                %s
                """.formatted(
                chunk.citationId(),
                chunk.title(),
                chunk.sourceType(),
                chunk.sourceId(),
                chunk.content()
        ), metadata);
    }

    private RagRetrievalResult buildRetrievalResult(String query, List<RagRetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return RagRetrievalResult.empty(query);
        }
        List<Document> documents = chunks.stream()
                .map(this::toDocument)
                .toList();
        return new RagRetrievalResult(query, chunks, documents);
    }

    private List<RagRetrievedChunk> toRecallOrderedChunks(List<KnowledgeSearchCandidate> recallCandidates, int finalTopK) {
        List<RagRetrievedChunk> finalChunks = new ArrayList<>();
        int citationIndex = 1;
        for (KnowledgeSearchCandidate candidate : recallCandidates) {
            finalChunks.add(new RagRetrievedChunk(
                    candidate.chunkId(),
                    "C" + citationIndex++,
                    candidate.sourceType(),
                    candidate.sourceId(),
                    candidate.title(),
                    candidate.content(),
                    snippet(candidate.content()),
                    candidate.similarityScore(),
                    candidate.metadata()
            ));
            if (finalChunks.size() >= finalTopK) {
                break;
            }
        }
        return finalChunks;
    }

    private String snippet(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 200 ? normalized : normalized.substring(0, 197) + "...";
    }

    private int resolveFinalTopK(SearchRequest searchRequest, int defaultFinalTopK) {
        if (searchRequest == null || searchRequest.getTopK() <= 0) {
            return defaultFinalTopK;
        }
        return Math.min(searchRequest.getTopK(), defaultFinalTopK);
    }

    private double resolveRecallThreshold(SearchRequest searchRequest, double defaultThreshold) {
        if (searchRequest == null) {
            return defaultThreshold;
        }
        double threshold = searchRequest.getSimilarityThreshold();
        if (threshold < 0 || threshold > 1) {
            return defaultThreshold;
        }
        return threshold;
    }

    private static final class FusionAccumulator {

        private final KnowledgeSearchCandidate candidate;
        private final Set<String> routes = new LinkedHashSet<>();
        private double fusionScore;
        private boolean exactLexicalMatch;

        private FusionAccumulator(KnowledgeSearchCandidate candidate) {
            this.candidate = candidate;
        }

        private void add(double contribution, String route, double routeScore) {
            fusionScore += contribution;
            routes.add(route);
            exactLexicalMatch = exactLexicalMatch || ("lexical".equals(route) && routeScore >= 0.98d);
        }

        private KnowledgeSearchCandidate candidate() {
            return candidate;
        }

        private Set<String> routes() {
            return routes;
        }

        private double fusionScore() {
            return fusionScore;
        }

        private boolean exactLexicalMatch() {
            return exactLexicalMatch;
        }
    }
}
