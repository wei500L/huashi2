package com.huashi.eftransfer.app.modules.ai.vo;

import java.time.LocalDateTime;

public record AiAsyncJobSubmitVO(
        String jobId,
        String scene,
        String status,
        LocalDateTime createdAt
) {
}
