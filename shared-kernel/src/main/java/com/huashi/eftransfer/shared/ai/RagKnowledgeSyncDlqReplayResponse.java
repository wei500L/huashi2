package com.huashi.eftransfer.shared.ai;

public record RagKnowledgeSyncDlqReplayResponse(
        int requestedLimit,
        int replayedCount,
        boolean drained
) {
}
