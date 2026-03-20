package com.huashi.eftransfer.shared.ai;

import java.util.List;

public record LexicalKnowledgeSenseItem(
        Long senseId,
        Integer sortOrder,
        String englishDefinition,
        String frenchDefinition,
        String chineseDefinition,
        List<LexicalKnowledgeExampleItem> examples
) {
}
