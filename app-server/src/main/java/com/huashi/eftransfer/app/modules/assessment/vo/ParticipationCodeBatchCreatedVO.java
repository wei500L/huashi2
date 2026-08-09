package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;
import java.util.List;

public record ParticipationCodeBatchCreatedVO(
        String batchId,
        LocalDateTime generatedAt,
        List<String> participationCodes
) {
}
