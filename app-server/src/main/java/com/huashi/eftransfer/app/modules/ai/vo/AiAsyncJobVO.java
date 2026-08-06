package com.huashi.eftransfer.app.modules.ai.vo;

import java.time.LocalDateTime;

public record AiAsyncJobVO(
        String jobId,
        String scene,
        String status,
        Object result,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
