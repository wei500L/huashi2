package com.huashi.eftransfer.app.modules.lexicon.imports.vo;

import java.time.LocalDateTime;

public record LexicalImportBatchDetailVO(
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
        String fileSha256,
        LocalDateTime parserJobStartedAt,
        LocalDateTime parserJobFinishedAt,
        LocalDateTime importJobStartedAt,
        LocalDateTime importJobFinishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
