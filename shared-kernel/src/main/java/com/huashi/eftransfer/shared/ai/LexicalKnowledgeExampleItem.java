package com.huashi.eftransfer.shared.ai;

public record LexicalKnowledgeExampleItem(
        Long exampleId,
        Integer sortOrder,
        String englishExample,
        String frenchExample,
        String chineseTranslation,
        String contextSupportLevel,
        String source
) {
}
