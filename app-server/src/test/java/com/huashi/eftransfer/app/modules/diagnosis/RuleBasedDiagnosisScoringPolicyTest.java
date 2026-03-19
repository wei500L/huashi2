package com.huashi.eftransfer.app.modules.diagnosis;

import com.huashi.eftransfer.app.modules.diagnosis.service.DiagnosisScoringPolicy;
import com.huashi.eftransfer.app.modules.diagnosis.service.RuleBasedDiagnosisScoringPolicy;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisOptionPayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisScoringProfilePayload;
import com.huashi.eftransfer.shared.enums.DiagnosisErrorType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedDiagnosisScoringPolicyTest {

    private final RuleBasedDiagnosisScoringPolicy scoringPolicy = new RuleBasedDiagnosisScoringPolicy();

    @Test
    void shouldAggregateMixedPerformanceIntoTransferMetrics() {
        DiagnosisScoringPolicy.AnsweredItem itemOne = answeredItem(
                1L,
                1,
                definition(100L, "COGNATE", "REACTION_TIME", "LOW", true, "semantic_match", 0.05),
                "semantic_match",
                520,
                60,
                true,
                true,
                null,
                0.08,
                0.87
        );
        DiagnosisScoringPolicy.AnsweredItem itemTwo = answeredItem(
                2L,
                2,
                definition(101L, "FALSE_FRIEND", "REACTION_TIME", "MEDIUM", false, "semantic_mismatch", 0.92),
                "semantic_match",
                1320,
                320,
                false,
                true,
                DiagnosisErrorType.FALSE_FRIEND_CONFUSION,
                0.95,
                0.04
        );
        DiagnosisScoringPolicy.AnsweredItem itemThree = answeredItem(
                3L,
                3,
                definition(102L, "FALSE_FRIEND", "SEMANTIC_JUDGEMENT", "HIGH", false, "currently_correct", 0.88),
                "actually_trap",
                1490,
                410,
                false,
                true,
                DiagnosisErrorType.CONTEXT_IGNORED,
                0.91,
                0.06
        );

        DiagnosisScoringPolicy.SummaryAggregation aggregation = scoringPolicy.aggregate(List.of(itemOne, itemTwo, itemThree));

        assertThat(aggregation.positiveTransferScore()).isGreaterThan(0.70);
        assertThat(aggregation.negativeTransferRisk()).isGreaterThan(0.80);
        assertThat(aggregation.contextSensitivity()).isBetween(0.0, 1.0);
        assertThat(aggregation.semanticDiscrimination()).isLessThan(0.40);
        assertThat(aggregation.overallAccuracy()).isEqualTo(0.3333, within(0.0002));
        assertThat(aggregation.averageReactionTime()).isEqualTo(1110);
        assertThat(aggregation.errorTypeDistribution()).hasSize(6);
        assertThat(aggregation.errorTypeDistribution().stream()
                .filter(item -> "false_friend_confusion".equals(item.code()))
                .findFirst()
                .orElseThrow()
                .count()).isEqualTo(1);
        assertThat(aggregation.highRiskLexicalPairs()).hasSize(2);
        assertThat(aggregation.highRiskLexicalPairs().getFirst().riskScore()).isGreaterThanOrEqualTo(
                aggregation.highRiskLexicalPairs().get(1).riskScore()
        );
        assertThat(aggregation.chartPayload().responseTimeline()).hasSize(3);
    }

    @Test
    void shouldEvaluatePositiveAndNegativeScoresWithRuleFormula() {
        DiagnosisScoringPolicy.ItemDefinition positiveDefinition = definition(100L, "COGNATE", "REACTION_TIME", "LOW", true, "semantic_match", 0.05);
        DiagnosisScoringPolicy.ItemDefinition negativeDefinition = definition(101L, "FALSE_FRIEND", "REACTION_TIME", "MEDIUM", false, "semantic_mismatch", 0.92);

        DiagnosisScoringPolicy.ItemEvaluation positiveEvaluation = scoringPolicy.evaluate(
                positiveDefinition,
                new DiagnosisScoringPolicy.SubmittedAnswer(true, null, 600, 80)
        );
        DiagnosisScoringPolicy.ItemEvaluation negativeEvaluation = scoringPolicy.evaluate(
                negativeDefinition,
                new DiagnosisScoringPolicy.SubmittedAnswer(true, null, 1400, 300)
        );

        assertThat(positiveEvaluation.correct()).isTrue();
        assertThat(positiveEvaluation.itemScore()).isGreaterThan(0.80);
        assertThat(positiveEvaluation.transferRiskScore()).isLessThan(0.20);

        assertThat(negativeEvaluation.correct()).isFalse();
        assertThat(negativeEvaluation.errorType()).isEqualTo(DiagnosisErrorType.FALSE_FRIEND_CONFUSION);
        assertThat(negativeEvaluation.transferRiskScore()).isGreaterThan(0.90);
        assertThat(negativeEvaluation.itemScore()).isLessThan(0.10);
    }

    private DiagnosisScoringPolicy.ItemDefinition definition(
            Long lexicalPairId,
            String lexicalPairType,
            String taskType,
            String contextSupportLevel,
            boolean expectedSemanticMatch,
            String correctAnswerKey,
            double falseFriendRisk
    ) {
        List<DiagnosisOptionPayload> options = taskType.equals("SEMANTIC_JUDGEMENT")
                ? List.of(
                new DiagnosisOptionPayload("actually_trap", "Actually", true, true),
                new DiagnosisOptionPayload("currently_correct", "Currently", false, false)
        )
                : List.of(
                new DiagnosisOptionPayload("semantic_match", "Semantic Match", true, false),
                new DiagnosisOptionPayload("semantic_mismatch", "Semantic Mismatch", false, false)
        );

        return new DiagnosisScoringPolicy.ItemDefinition(
                lexicalPairId,
                lexicalPairType,
                taskType,
                contextSupportLevel,
                expectedSemanticMatch,
                correctAnswerKey,
                options,
                new DiagnosisScoringProfilePayload("RULE_V1", null, 1.0, taskType.equals("REACTION_TIME") ? 1500 : 2000),
                "english-" + lexicalPairId,
                "french-" + lexicalPairId,
                falseFriendRisk,
                0.30
        );
    }

    private DiagnosisScoringPolicy.AnsweredItem answeredItem(
            Long itemResultId,
            int presentationOrder,
            DiagnosisScoringPolicy.ItemDefinition definition,
            String selectedAnswerKey,
            int reactionTimeMs,
            int hesitationTimeMs,
            boolean correct,
            boolean semanticConsistent,
            DiagnosisErrorType errorType,
            double transferRiskScore,
            double itemScore
    ) {
        return new DiagnosisScoringPolicy.AnsweredItem(
                itemResultId,
                presentationOrder,
                definition,
                selectedAnswerKey,
                reactionTimeMs,
                hesitationTimeMs,
                correct,
                semanticConsistent,
                errorType,
                transferRiskScore,
                itemScore
        );
    }

    private org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }
}
