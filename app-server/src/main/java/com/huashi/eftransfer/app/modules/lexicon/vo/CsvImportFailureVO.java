package com.huashi.eftransfer.app.modules.lexicon.vo;

public record CsvImportFailureVO(
        long rowNumber,
        String englishWord,
        String frenchWord,
        String reason
) {
}
