package com.huashi.eftransfer.ai.modules.rag.service;

import com.huashi.eftransfer.ai.common.runtime.AiRuntimeConfigService;
import com.huashi.eftransfer.ai.integration.provider.AiProviderRegistry;
import com.huashi.eftransfer.ai.modules.rag.repository.KnowledgeStoreRepository;
import com.huashi.eftransfer.ai.modules.rag.support.KnowledgeSearchCandidate;
import com.huashi.eftransfer.ai.modules.rag.support.RagRetrievedChunk;
import com.huashi.eftransfer.ai.modules.rag.support.RagRetrievalResult;
import com.huashi.eftransfer.ai.modules.rag.support.RagSearchFilter;
import com.huashi.eftransfer.shared.ai.EmbeddingRequest;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeSearchService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchService.class);

    private final AiProviderRegistry aiProviderRegistry;
    private final KnowledgeStoreRepository knowledgeStoreRepository;
    private final AiRuntimeConfigService runtimeConfigService;

    public KnowledgeSearchService(
            AiProviderRegistry aiProviderRegistry,
            KnowledgeStoreRepository knowledgeStoreRepository,
            AiRuntimeConfigService runtimeConfigService
    ) {
        this.aiProviderRegistry = aiProviderRegistry;
        this.knowledgeStoreRepository = knowledgeStoreRepository;
        this.runtimeConfigService = runtimeConfigService;
    }

    public RagRetrievalResult search(String query, RagSearchFilter filter) {
        return search(query, filter, null);
    }

    public RagRetrievalResult search(String query, RagSearchFilter filter, SearchRequest searchRequest) {
        var retrieval = runtimeConfigService.current().config().rag().retrieval();
        EmbeddingResponse embeddingResponse = aiProviderRegistry.embed(new EmbeddingRequest(query, null, null));
        if (embeddingResponse.items() == null || embeddingResponse.items().isEmpty()) {
            return RagRetrievalResult.empty(query);
        }

        List<Double> queryEmbedding = embeddingResponse.items().getFirst().embedding();
        int finalTopK = resolveFinalTopK(searchRequest, retrieval.finalTopK());
        double recallThreshold = resolveRecallThreshold(searchRequest, retrieval.recallThreshold());
        List<KnowledgeSearchCandidate> recallCandidates = knowledgeStoreRepository.similaritySearch(
                queryEmbedding,
                filter,
                retrieval.recallTopK(),
                retrieval.hnswEfSearch()
        ).stream()
                .filter(candidate -> candidate.similarityScore() >= recallThreshold)
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
        if (rerankResponse.items() != null) {
            int citationIndex = 1;
            for (RerankItem rerankItem : rerankResponse.items()) {
                if (rerankItem.relevanceScore() < retrieval.rerankThreshold()) {
                    continue;
                }
                KnowledgeSearchCandidate candidate = candidateByIndex.get(rerankItem.index());
                if (candidate == null) {
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
                        rerankItem.relevanceScore(),
                        candidate.metadata()
                ));
                if (finalChunks.size() >= finalTopK) {
                    break;
                }
            }
        }

        return buildRetrievalResult(query, finalChunks);
    }

    private String toRerankDocument(KnowledgeSearchCandidate candidate) {
        return """
                Title: %s
                Source Type: %s
                Source Id: %s
                Content:
                %s
                """.formatted(candidate.title(), candidate.sourceType(), candidate.sourceId(), candidate.content());
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
}
