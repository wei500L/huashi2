package com.huashi.eftransfer.app.modules.lexicon.vo;

public record LexicalListItemVO(
        Long itemId,
        Long lexicalPairId,
        Integer sortOrder,
        String notes,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        String lexicalPairType,
        String defaultContextSupport,
        Integer difficultyLevel,
        String riskLevel
) {
}
