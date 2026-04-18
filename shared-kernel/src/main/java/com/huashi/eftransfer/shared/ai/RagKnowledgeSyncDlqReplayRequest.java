package com.huashi.eftransfer.shared.ai;

import jakarta.validation.constraints.Positive;

public record RagKnowledgeSyncDlqReplayRequest(
        @Positive(message = "limit must be greater than 0")
        Integer limit
) {
}
