package com.huashi.eftransfer.app.modules.assessment.dto;

import jakarta.validation.constraints.NotBlank;

public record ResearchExportRequest(
        @NotBlank String format,
        String scope,
        String status,
        String entryType,
        String qualityFlag,
        String aiStatus,
        String submittedFrom,
        String submittedTo,
        String keyword,
        boolean includeSensitiveFields,
        boolean includeAttachmentManifest
) {
}
