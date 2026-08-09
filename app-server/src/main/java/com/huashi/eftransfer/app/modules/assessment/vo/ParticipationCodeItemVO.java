package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record ParticipationCodeItemVO(
        Long codeId,
        String codeHint,
        String status,
        String exportBatchId,
        LocalDateTime exportedAt,
        LocalDateTime firstVerifiedAt,
        LocalDateTime lastVerifiedAt,
        LocalDateTime submittedAt,
        String lastVerifiedIp
) {
}
