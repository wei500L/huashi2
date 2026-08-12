package com.huashi.eftransfer.app.modules.practice.vo;

import java.time.LocalDateTime;
import java.util.List;

public record PracticeSessionDetailVO(
        Long sessionId,
        String bankCode,
        String sectionCode,
        String status,
        Integer totalCount,
        Integer answeredCount,
        Integer correctCount,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        List<PracticeQuestionVO> questions
) {
}
