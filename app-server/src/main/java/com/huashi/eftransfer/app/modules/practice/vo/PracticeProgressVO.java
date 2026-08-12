package com.huashi.eftransfer.app.modules.practice.vo;

public record PracticeProgressVO(
        Long sessionId,
        String status,
        Integer totalCount,
        Integer answeredCount,
        Integer correctCount
) {
}
