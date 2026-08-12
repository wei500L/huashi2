package com.huashi.eftransfer.app.modules.assessment.vo;

public record ResearchFileInitiateVO(
        String uploadToken,
        Long fileId,
        long maxFileBytes,
        int maxFilesPerQuestion
) {
}
