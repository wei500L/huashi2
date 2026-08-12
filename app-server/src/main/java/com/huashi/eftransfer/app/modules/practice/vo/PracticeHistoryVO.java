package com.huashi.eftransfer.app.modules.practice.vo;

import java.time.LocalDateTime;

public record PracticeHistoryVO(
        Long sessionId,
        String bankCode,
        String sectionCode,
        String status,
        Integer totalCount,
        Integer answeredCount,
        Integer correctCount,
        Double percentage,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {
}
