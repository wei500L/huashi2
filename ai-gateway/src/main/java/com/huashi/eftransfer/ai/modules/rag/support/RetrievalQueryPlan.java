package com.huashi.eftransfer.ai.modules.rag.support;

import java.util.List;

public record RetrievalQueryPlan(
        List<String> semanticQueries,
        List<String> lexicalTerms
) {

    public RetrievalQueryPlan {
        semanticQueries = semanticQueries == null ? List.of() : List.copyOf(semanticQueries);
        lexicalTerms = lexicalTerms == null ? List.of() : List.copyOf(lexicalTerms);
    }
}
