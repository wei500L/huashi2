package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record ResearchAttachmentVO(
        Long fileId,
        String uploadToken,
        String originalFileName,
        String mimeType,
        String fileExtension,
        Long sizeBytes,
        String scanStatus,
        String bindingStatus,
        LocalDateTime uploadedAt,
        boolean downloadable
) {
}
