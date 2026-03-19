package com.huashi.eftransfer.app.modules.lexicon.vo;

public record LexicalPairExampleVO(
        Long id,
        Integer sortOrder,
        String englishExample,
        String frenchExample,
        String chineseTranslation,
        String contextSupportLevel,
        String source
) {
}
