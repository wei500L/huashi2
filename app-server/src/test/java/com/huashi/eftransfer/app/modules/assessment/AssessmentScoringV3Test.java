package com.huashi.eftransfer.app.modules.assessment;

import com.huashi.eftransfer.app.modules.assessment.service.AssessmentScoringV3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AssessmentScoringV3Test {

    @Test
    void reportsEachFf4TypeAsSeparateDimension() {
        List<AssessmentScoringV3.Question> questions = List.of(
                new AssessmentScoringV3.Question(1, "SINGLE_CHOICE", 1, 1d,
                        "FF4_WORD_MEANING", "WORD", "FALSE_FRIEND", List.of("A")),
                new AssessmentScoringV3.Question(2, "SINGLE_CHOICE", 1, 1d,
                        "FF4_SENTENCE_SYNONYM", "SENTENCE", "FALSE_FRIEND", List.of("B")),
                new AssessmentScoringV3.Question(3, "TRUE_FALSE", 1, 1d,
                        "FF4_TRUE_FALSE_TRANSFER", "WORD", "FALSE_FRIEND", List.of("F")),
                new AssessmentScoringV3.Question(4, "SPELLING", 1, 1d,
                        "FF4_SPELLING", "WORD", "FALSE_FRIEND", List.of("paradis"))
        );
        AssessmentScoringV3.Result result = AssessmentScoringV3.score(questions, Map.of(
                1, new AssessmentScoringV3.Response(List.of("A"), 800L, null, false, null, null),
                2, new AssessmentScoringV3.Response(List.of("B"), 900L, null, false, null, null),
                3, new AssessmentScoringV3.Response(List.of("F"), 600L, "because it is false", false, null, null),
                4, new AssessmentScoringV3.Response(List.of("paradis"), 1_200L, null, false, 700L, 500L)
        ));

        assertThat(result.correctCount()).isEqualTo(4);
        assertThat(result.dimensions().keySet()).containsExactlyInAnyOrder(
                "FF4_WORD_MEANING", "FF4_SENTENCE_SYNONYM", "FF4_TRUE_FALSE_TRANSFER", "FF4_SPELLING");
        assertThat(result.dimensions().get("FF4_SPELLING").ratio()).isEqualTo(1d);
        assertThat(result.spelling()).isNotNull();
        assertThat(result.spelling().firstTryCorrectCount()).isEqualTo(1);
        assertThat(result.spelling().hintCorrectCount()).isZero();
        assertThat(result.spelling().preHintMedianMs()).isEqualTo(700L);
        assertThat(result.spelling().postHintMedianMs()).isEqualTo(500L);
    }

    @Test
    void distinguishesFirstTryCorrectFromHintCorrect() {
        List<AssessmentScoringV3.Question> questions = List.of(
                new AssessmentScoringV3.Question(1, "SPELLING", 1, 1d,
                        "FF4_SPELLING", "WORD", "FALSE_FRIEND", List.of("chasse")),
                new AssessmentScoringV3.Question(2, "SPELLING", 1, 1d,
                        "FF4_SPELLING", "WORD", "FALSE_FRIEND", List.of("garder"))
        );
        AssessmentScoringV3.Result result = AssessmentScoringV3.score(questions, Map.of(
                1, new AssessmentScoringV3.Response(List.of("chasse"), 1_000L, null, false, null, null),
                2, new AssessmentScoringV3.Response(List.of("garder"), 2_000L, null, true, 1_200L, 800L)
        ));

        assertThat(result.spelling().firstTryCorrectCount()).isEqualTo(1);
        assertThat(result.spelling().hintCorrectCount()).isEqualTo(1);
        assertThat(result.spelling().preHintMedianMs()).isEqualTo(1_200L);
        assertThat(result.spelling().postHintMedianMs()).isEqualTo(800L);
    }

    @Test
    void keepsV1StyleAggregatesForComparability() {
        List<AssessmentScoringV3.Question> questions = List.of(
                new AssessmentScoringV3.Question(1, "SINGLE_CHOICE", 1, 1d,
                        "FF4_WORD_MEANING", "WORD", "COGNATE", List.of("A")),
                new AssessmentScoringV3.Question(2, "SINGLE_CHOICE", 1, 1d,
                        "FF4_SENTENCE_SYNONYM", "SENTENCE", "FRENCH_CONTROL", List.of("B"))
        );
        AssessmentScoringV3.Result result = AssessmentScoringV3.score(questions, Map.of(
                1, new AssessmentScoringV3.Response(List.of("A"), 900L, null, false, null, null),
                2, new AssessmentScoringV3.Response(List.of("B"), 900L, null, false, null, null)
        ));

        assertThat(result.falseFriendInterferencePoints()).isNull();
        assertThat(result.contextRepairPoints()).isEqualTo(0d);
    }

    @Test
    void spellingMatchingUsesTheSameAccentFoldingAsSavedResponses() {
        List<AssessmentScoringV3.Question> questions = List.of(
                new AssessmentScoringV3.Question(1, "SPELLING", 1, 1d,
                        "FF4_SPELLING", "WORD", "FALSE_FRIEND", List.of("théorie")),
                new AssessmentScoringV3.Question(2, "SPELLING", 1, 1d,
                        "FF4_SPELLING", "WORD", "FALSE_FRIEND", List.of("reconnaître"))
        );

        AssessmentScoringV3.Result result = AssessmentScoringV3.score(questions, Map.of(
                1, new AssessmentScoringV3.Response(List.of("theorie"), 500L, null, false, null, null),
                2, new AssessmentScoringV3.Response(List.of("RECONNAITRE"), 500L, null, false, null, null)
        ));

        assertThat(result.correctCount()).isEqualTo(2);
    }
}
