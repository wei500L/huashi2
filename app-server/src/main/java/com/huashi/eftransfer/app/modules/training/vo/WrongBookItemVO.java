package com.huashi.eftransfer.app.modules.training.vo;

import java.time.LocalDateTime;

public record WrongBookItemVO(
        Long wrongBookId,
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        String lexicalPairType,
        Integer wrongCount,
        String lastErrorType,
        String masteryStatus,
        LocalDateTime firstWrongAt,
        LocalDateTime lastWrongAt,
        LocalDateTime nextReviewAt
) {
}
