package com.huashi.eftransfer.ai.modules.rag.support;

public final class EmbeddingTextSupport {

    private static final String QUERY_EMBEDDING_INSTRUCTION =
            "Given an English, French, or Chinese lexical-transfer learning question, retrieve passages that provide directly relevant lexical evidence.";

    private EmbeddingTextSupport() {
    }

    public static String toRetrievalQuery(String query) {
        return "Instruct: %s\nQuery:%s".formatted(QUERY_EMBEDDING_INSTRUCTION, query);
    }
}
