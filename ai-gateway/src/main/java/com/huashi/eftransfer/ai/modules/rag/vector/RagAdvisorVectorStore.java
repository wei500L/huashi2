package com.huashi.eftransfer.ai.modules.rag.vector;

import com.huashi.eftransfer.ai.modules.rag.service.KnowledgeSearchService;
import com.huashi.eftransfer.ai.modules.rag.service.RagRetrievalCapture;
import com.huashi.eftransfer.ai.modules.rag.support.RagRetrievalResult;
import com.huashi.eftransfer.ai.modules.rag.support.RagSearchFilter;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;

public class RagAdvisorVectorStore implements VectorStore {

    private final KnowledgeSearchService knowledgeSearchService;
    private final RagRetrievalCapture ragRetrievalCapture;
    private final RagSearchFilter ragSearchFilter;

    public RagAdvisorVectorStore(
            KnowledgeSearchService knowledgeSearchService,
            RagRetrievalCapture ragRetrievalCapture,
            RagSearchFilter ragSearchFilter
    ) {
        this.knowledgeSearchService = knowledgeSearchService;
        this.ragRetrievalCapture = ragRetrievalCapture;
        this.ragSearchFilter = ragSearchFilter;
    }

    @Override
    public void add(List<Document> documents) {
        throw new UnsupportedOperationException("RagAdvisorVectorStore does not support add");
    }

    @Override
    public void delete(List<String> idList) {
        throw new UnsupportedOperationException("RagAdvisorVectorStore does not support delete");
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        throw new UnsupportedOperationException("RagAdvisorVectorStore does not support delete");
    }

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        RagRetrievalResult retrievalResult = knowledgeSearchService.search(request.getQuery(), ragSearchFilter, request);
        ragRetrievalCapture.store(retrievalResult);
        return retrievalResult.documents();
    }

    @Override
    public List<Document> similaritySearch(String query) {
        RagRetrievalResult retrievalResult = knowledgeSearchService.search(query, ragSearchFilter);
        ragRetrievalCapture.store(retrievalResult);
        return retrievalResult.documents();
    }
}
