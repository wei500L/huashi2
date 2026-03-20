package com.huashi.eftransfer.ai.modules.rag.service;

import com.huashi.eftransfer.ai.integration.provider.AiProviderRegistry;
import com.huashi.eftransfer.ai.modules.rag.config.RagProperties;
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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class KnowledgeSearchService {

    private final AiProviderRegistry aiProviderRegistry;
    private final KnowledgeStoreRepository knowledgeStoreRepository;
    private final RagProperties ragProperties;

    public KnowledgeSearchService(
            AiProviderRegistry aiProviderRegistry,
            KnowledgeStoreRepository knowledgeStoreRepository,
            RagProperties ragProperties
    ) {
        this.aiProviderRegistry = aiProviderRegistry;
        this.knowledgeStoreRepository = knowledgeStoreRepository;
        this.ragProperties = ragProperties;
    }

    public RagRetrievalResult search(String query, RagSearchFilter filter) {
        EmbeddingResponse embeddingResponse = aiProviderRegistry.resolveActiveProvider()
                .embed(new EmbeddingRequest(query, null, null));
        if (embeddingResponse.items() == null || embeddingResponse.items().isEmpty()) {
            return RagRetrievalResult.empty(query);
        }

        List<Double> queryEmbedding = embeddingResponse.items().getFirst().embedding();
        List<KnowledgeSearchCandidate> recallCandidates = knowledgeStoreRepository.similaritySearch(
                toVectorLiteral(queryEmbedding),
                filter,
                ragProperties.getRetrieval().getRecallTopK()
        ).stream()
                .filter(candidate -> candidate.similarityScore() >= ragProperties.getRetrieval().getRecallThreshold())
                .toList();

        if (recallCandidates.isEmpty()) {
            return RagRetrievalResult.empty(query);
        }

        List<String> rerankDocuments = recallCandidates.stream()
                .map(this::toRerankDocument)
                .toList();
        RerankResponse rerankResponse = aiProviderRegistry.resolveActiveProvider().rerank(new RerankRequest(
                null,
                query,
                rerankDocuments,
                Math.min(ragProperties.getRetrieval().getRerankTopN(), rerankDocuments.size()),
                Boolean.TRUE,
                "Rank lexical transfer knowledge by relevance to the user query."
        ));

        Map<Integer, KnowledgeSearchCandidate> candidateByIndex = new LinkedHashMap<>();
        for (int index = 0; index < recallCandidates.size(); index++) {
            candidateByIndex.put(index, recallCandidates.get(index));
        }

        List<RagRetrievedChunk> finalChunks = new ArrayList<>();
        if (rerankResponse.items() != null) {
            int citationIndex = 1;
            for (RerankItem rerankItem : rerankResponse.items()) {
                if (rerankItem.relevanceScore() < ragProperties.getRetrieval().getRerankThreshold()) {
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
                if (finalChunks.size() >= ragProperties.getRetrieval().getFinalTopK()) {
                    break;
                }
            }
        }

        if (finalChunks.isEmpty()) {
            return RagRetrievalResult.empty(query);
        }

        List<Document> documents = finalChunks.stream()
                .map(this::toDocument)
                .toList();
        return new RagRetrievalResult(query, finalChunks, documents);
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

    private String snippet(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 200 ? normalized : normalized.substring(0, 197) + "...";
    }

    private String toVectorLiteral(List<Double> embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < embedding.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.ROOT, "%.12f", embedding.get(index)));
        }
        return builder.append(']').toString();
    }
}
