package com.huashi.eftransfer.app.modules.lexicon.imports.support;

public record LexicalImportCounts(
        int totalRows,
        int readyRows,
        int invalidRows,
        int skippedRows,
        int importedRows
) {
}
