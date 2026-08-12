package com.huashi.eftransfer.app.modules.practice.vo;

public record PracticeSessionCreatedVO(
        Long sessionId,
        String bankCode,
        String sectionCode,
        Integer totalCount
) {
}
