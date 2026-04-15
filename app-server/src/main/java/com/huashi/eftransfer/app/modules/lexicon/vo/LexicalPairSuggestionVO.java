package com.huashi.eftransfer.app.modules.lexicon.vo;

public record LexicalPairSuggestionVO(
        Long id,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        String lexicalPairType,
        String matchedBy
) {
}
