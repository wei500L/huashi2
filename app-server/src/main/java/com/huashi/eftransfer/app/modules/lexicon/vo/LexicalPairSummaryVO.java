package com.huashi.eftransfer.app.modules.lexicon.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LexicalPairSummaryVO(
        Long id,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        String lexicalPairType,
        BigDecimal semanticOverlapScore,
        BigDecimal falseFriendRisk,
        String riskLevel,
        String defaultContextSupport,
        Integer difficultyLevel,
        String source,
        Boolean active,
        String knowledgeStatus,
        String embeddingStatus,
        LocalDateTime lastEmbeddedAt,
        List<String> tags
) {
}
