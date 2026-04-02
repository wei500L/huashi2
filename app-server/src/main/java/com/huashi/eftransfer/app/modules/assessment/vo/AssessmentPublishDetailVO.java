package com.huashi.eftransfer.app.modules.assessment.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AssessmentPublishDetailVO(
        Long publishId,
        Long paperId,
        String paperTitle,
        String paperDescription,
        Long teachingClassId,
        String className,
        String status,
        Integer durationMinutes,
        Integer questionCount,
        Integer totalScore,
        String instructionsText,
        LocalDateTime startsAt,
        LocalDateTime dueAt,
        LocalDateTime publishedAt,
        Integer assignedCount,
        Integer notStartedCount,
        Integer inProgressCount,
        Integer submittedCount,
        Double averageScore,
        List<AssessmentPublishRosterItemVO> roster
) {
}
