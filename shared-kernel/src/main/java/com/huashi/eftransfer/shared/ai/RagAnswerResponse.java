package com.huashi.eftransfer.shared.ai;

import java.util.List;

public record RagAnswerResponse(
        String answer,
        boolean grounded,
        String uncertaintyNote,
        List<RagCitation> citations,
        List<RagContextChunk> contextChunks
) {
}
