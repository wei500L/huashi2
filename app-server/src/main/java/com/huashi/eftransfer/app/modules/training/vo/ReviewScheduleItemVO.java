package com.huashi.eftransfer.app.modules.training.vo;

import java.time.LocalDateTime;

public record ReviewScheduleItemVO(
        Long reviewScheduleId,
        Long wrongBookId,
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        String lexicalPairType,
        Integer scheduleStage,
        Integer intervalDays,
        LocalDateTime dueAt,
        String status,
        String reviewMode,
        String triggerReason
) {
}
