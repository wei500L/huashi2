package com.huashi.eftransfer.app.modules.lexicon.vo;

import java.util.List;

public record AddLexicalListItemsResultVO(
        int addedCount,
        List<Long> skippedPairIds
) {
}
