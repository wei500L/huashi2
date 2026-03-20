package com.huashi.eftransfer.ai.modules.rag.support;

import org.springframework.ai.document.Document;

import java.util.List;

public record RagRetrievalResult(
        String query,
        List<RagRetrievedChunk> chunks,
        List<Document> documents
) {

    public static RagRetrievalResult empty(String query) {
        return new RagRetrievalResult(query, List.of(), List.of());
    }
}
