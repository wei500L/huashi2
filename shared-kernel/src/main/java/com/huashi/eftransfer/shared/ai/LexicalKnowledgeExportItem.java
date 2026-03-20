package com.huashi.eftransfer.shared.ai;

import java.time.OffsetDateTime;
import java.util.List;

public record LexicalKnowledgeExportItem(
        Long lexicalPairId,
        OffsetDateTime sourceUpdatedAt,
        Boolean active,
        String knowledgeStatus,
        String embeddingStatus,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        String lexicalPairType,
        Double semanticOverlapScore,
        Double falseFriendRisk,
        String defaultContextSupport,
        Integer difficultyLevel,
        String notes,
        String source,
        List<String> tags,
        List<LexicalKnowledgeSenseItem> senses
) {
}
