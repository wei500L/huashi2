package com.huashi.eftransfer.app.modules.diagnosis.event;

import java.time.LocalDateTime;
import java.util.List;

public record DiagnosisCompletedEvent(
        Long sessionId,
        Long summaryId,
        Long templateId,
        Long ownerUserId,
        LocalDateTime completedAt,
        double positiveTransferScore,
        double negativeTransferRisk,
        double contextSensitivity,
        double semanticDiscrimination,
        double overallAccuracy,
        long averageReactionTime,
        List<HighRiskLexicalPairPayload> highRiskLexicalPairs,
        String traceId,
        int eventVersion
) {

    public record HighRiskLexicalPairPayload(
            Long lexicalPairId,
            String englishWord,
            String frenchWord,
            double riskScore,
            String dominantErrorType
    ) {
    }
}
