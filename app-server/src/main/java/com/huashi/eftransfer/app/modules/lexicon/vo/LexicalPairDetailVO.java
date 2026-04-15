package com.huashi.eftransfer.app.modules.lexicon.vo;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LexicalPairDetailVO(
        Long id,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        String lexicalPairType,
        @NotNull
        BigDecimal semanticOverlapScore,
        @NotNull
        BigDecimal falseFriendRisk,
        String riskLevel,
        String defaultContextSupport,
        Integer difficultyLevel,
        String notes,
        String source,
        Boolean active,
        String searchableText,
        String knowledgeStatus,
        String embeddingStatus,
        LocalDateTime lastEmbeddedAt,
        List<String> tags,
        List<LexicalPairSenseVO> senses
) {
}
