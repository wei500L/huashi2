package com.huashi.eftransfer.app.modules.practice.vo;

import java.time.LocalDateTime;
import java.util.List;

public record PracticeResultVO(
        Long sessionId,
        String bankCode,
        String sectionCode,
        String status,
        Integer totalCount,
        Integer answeredCount,
        Integer correctCount,
        Double percentage,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String tutoringStatus,
        String tutoringJson,
        List<PracticeSectionMetricVO> sectionMetrics,
        List<PracticeResultQuestionVO> questions
) {
}
