package com.huashi.eftransfer.app.modules.lexicon.imports.vo;

import java.time.LocalDateTime;

public record LexicalImportBatchSummaryVO(
        Long id,
        String status,
        String sourceFormat,
        String originalFilename,
        String contentType,
        Long fileSizeBytes,
        int totalRows,
        int readyRows,
        int invalidRows,
        int skippedRows,
        int importedRows,
        String errorMessage,
        Long ownerUserId,
        String ownerDisplayName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime parserJobFinishedAt,
        LocalDateTime importJobFinishedAt
) {
}
