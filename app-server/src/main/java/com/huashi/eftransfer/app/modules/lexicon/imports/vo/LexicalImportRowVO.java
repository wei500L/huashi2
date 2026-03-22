package com.huashi.eftransfer.app.modules.lexicon.imports.vo;

import com.huashi.eftransfer.app.modules.lexicon.imports.support.LexicalImportRowDraft;

import java.util.List;

public record LexicalImportRowVO(
        Long id,
        Integer rowNumber,
        String status,
        LexicalImportRowDraft draft,
        List<String> validationErrors,
        Long importedLexicalPairId,
        String importMessage
) {
}
