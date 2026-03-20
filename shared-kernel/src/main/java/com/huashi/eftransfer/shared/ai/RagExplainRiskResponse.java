package com.huashi.eftransfer.shared.ai;

import java.util.List;

public record RagExplainRiskResponse(
        String riskExplanation,
        String negativeTransferReason,
        String priorityTrainingFocus,
        String uncertaintyNote,
        List<RagCitation> citations,
        List<RagContextChunk> contextChunks
) {
}
