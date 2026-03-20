package com.huashi.eftransfer.shared.ai;

import java.util.List;

public record RagRetrieveResponse(
        boolean grounded,
        String uncertaintyNote,
        List<RagCitation> citations,
        List<RagContextChunk> contextChunks
) {
}
