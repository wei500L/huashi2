package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record ResearchExportJobVO(
        Long jobId,
        String jobKey,
        Long publishId,
        String status,
        String format,
        String scope,
        boolean includeSensitiveFields,
        boolean includeAttachmentManifest,
        String fileName,
        String downloadPath,
        String errorMessage,
        LocalDateTime requestedAt,
        LocalDateTime completedAt
) {
}
