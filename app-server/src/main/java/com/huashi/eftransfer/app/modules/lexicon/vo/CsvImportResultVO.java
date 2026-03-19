package com.huashi.eftransfer.app.modules.lexicon.vo;

import java.util.List;

public record CsvImportResultVO(
        int successCount,
        int failedCount,
        List<CsvImportFailureVO> failures
) {
}
