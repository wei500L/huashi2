package com.huashi.eftransfer.app.modules.assessment;

import com.huashi.eftransfer.app.modules.assessment.service.AssessmentScoringV1;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AssessmentScoringV1Test {

    @Test
    void appliesWeightsAndRequiresExactMultipleChoiceMatch() {
        List<AssessmentScoringV1.Question> questions = List.of(
                new AssessmentScoringV1.Question(1, "SINGLE_CHOICE", 1, 1d, "WORD", "WORD", "COGNATE", List.of("A")),
                new AssessmentScoringV1.Question(2, "MULTIPLE_CHOICE", 2, 2d, "PHRASE", "PHRASE", "FALSE_FRIEND", List.of("A", "C"))
        );
        AssessmentScoringV1.Result result = AssessmentScoringV1.score(questions, Map.of(
                1, new AssessmentScoringV1.Response(List.of("A"), 500L, null),
                2, new AssessmentScoringV1.Response(List.of("C", "A"), 700L, null)
        ));

        assertThat(result.correctCount()).isEqualTo(2);
        assertThat(result.percentage()).isEqualTo(100d);
        assertThat(result.dimensions().get("WORD").ratio()).isEqualTo(1d);
        assertThat(result.reactionTime()).isNotNull();
    }

    @Test
    void emitsNullForEmptyDenominatorAndFlagsFastItems() {
        AssessmentScoringV1.Question question = new AssessmentScoringV1.Question(
                1, "SINGLE_CHOICE", 0, 1d, "", "", "", List.of("A"));
        AssessmentScoringV1.Result result = AssessmentScoringV1.score(List.of(question), Map.of(
                1, new AssessmentScoringV1.Response(List.of("A"), 250L, null)
        ));

        assertThat(result.percentage()).isNull();
        assertThat(result.qualityFlags()).contains("FAST_ITEM", "SHORT_TOTAL_DURATION");
    }

    @Test
    void returnsNullDeltaWhenEitherComparedGroupHasNoItems() {
        AssessmentScoringV1.Result result = AssessmentScoringV1.score(List.of(
                new AssessmentScoringV1.Question(1, "SINGLE_CHOICE", 1, 1d,
                        "WORD", "WORD", "COGNATE", List.of("A"))
        ), Map.of(1, new AssessmentScoringV1.Response(List.of("A"), 700_000L, null)));

        assertThat(result.cognateAdvantagePoints()).isNull();
        assertThat(result.falseFriendInterferencePoints()).isNull();
        assertThat(result.contextRepairPoints()).isNull();
    }
}
