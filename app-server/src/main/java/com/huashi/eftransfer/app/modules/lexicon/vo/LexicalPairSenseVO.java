package com.huashi.eftransfer.app.modules.lexicon.vo;

import java.util.List;

public record LexicalPairSenseVO(
        Long id,
        Integer sortOrder,
        String englishDefinition,
        String frenchDefinition,
        String chineseDefinition,
        List<LexicalPairExampleVO> examples
) {
}
