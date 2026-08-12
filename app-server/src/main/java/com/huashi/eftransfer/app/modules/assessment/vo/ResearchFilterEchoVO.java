package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;

public record ResearchFilterEchoVO(
        String status,
        String entryType,
        String qualityFlag,
        String aiStatus,
        LocalDateTime submittedFrom,
        LocalDateTime submittedTo,
        String keyword
) {
}
