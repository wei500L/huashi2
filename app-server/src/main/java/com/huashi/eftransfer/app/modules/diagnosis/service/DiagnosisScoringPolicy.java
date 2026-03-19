package com.huashi.eftransfer.app.modules.diagnosis.service;

import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisChartPayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisDistributionItem;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisHighRiskLexicalPair;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisOptionPayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisScoringProfilePayload;
import com.huashi.eftransfer.shared.enums.DiagnosisErrorType;

import java.util.List;

public interface DiagnosisScoringPolicy {

    ItemEvaluation evaluate(ItemDefinition definition, SubmittedAnswer answer);

    SummaryAggregation aggregate(List<AnsweredItem> answeredItems);

    record ItemDefinition(
            Long lexicalPairId,
            String lexicalPairType,
            String taskType,
            String contextSupportLevel,
            boolean expectedSemanticMatch,
            String correctAnswerKey,
            List<DiagnosisOptionPayload> options,
            DiagnosisScoringProfilePayload scoringProfile,
            String englishWord,
            String frenchWord,
            double falseFriendRisk,
            double semanticOverlapScore
    ) {
    }

    record SubmittedAnswer(
            Boolean selectedSemanticMatch,
            String selectedAnswerKey,
            int reactionTimeMs,
            int hesitationTimeMs
    ) {
    }

    record ItemEvaluation(
            String selectedAnswerKey,
            boolean correct,
            boolean semanticConsistent,
            DiagnosisErrorType errorType,
            double transferRiskScore,
            double itemScore
    ) {
    }

    record AnsweredItem(
            Long itemResultId,
            int presentationOrder,
            ItemDefinition definition,
            String selectedAnswerKey,
            int reactionTimeMs,
            int hesitationTimeMs,
            boolean correct,
            boolean semanticConsistent,
            DiagnosisErrorType errorType,
            double transferRiskScore,
            double itemScore
    ) {
    }

    record SummaryAggregation(
            double positiveTransferScore,
            double negativeTransferRisk,
            double contextSensitivity,
            double semanticDiscrimination,
            double overallAccuracy,
            long averageReactionTime,
            List<DiagnosisDistributionItem> errorTypeDistribution,
            List<DiagnosisHighRiskLexicalPair> highRiskLexicalPairs,
            DiagnosisChartPayload chartPayload
    ) {
    }
}
