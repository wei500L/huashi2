package com.huashi.eftransfer.app.modules.training;

import com.huashi.eftransfer.app.modules.training.service.RuleBasedTrainingRecommendationEngine;
import com.huashi.eftransfer.app.modules.training.service.TrainingRecommendationEngine;
import com.huashi.eftransfer.shared.enums.RiskLevel;
import com.huashi.eftransfer.shared.enums.TrainingMode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedTrainingRecommendationEngineTest {

    private final RuleBasedTrainingRecommendationEngine engine = new RuleBasedTrainingRecommendationEngine();

    @Test
    void shouldPrioritizeFalseFriendDiscriminationWhenNegativeTransferDominates() {
        TrainingRecommendationEngine.RecommendationContext context = new TrainingRecommendationEngine.RecommendationContext(
                1L,
                10L,
                20L,
                0.72,
                0.66,
                0.58,
                1380L,
                List.of(
                        signal(100L, "FALSE_FRIEND", 0.84, 3, 0, 0, 2, "FALSE_FRIEND_CONFUSION"),
                        signal(101L, "ORTHOGRAPHIC_SIMILAR", 0.76, 2, 0, 0, 1, "ORTHOGRAPHIC_INTERFERENCE")
                )
        );

        TrainingRecommendationEngine.TrainingRecommendation recommendation = engine.recommend(context);

        assertThat(recommendation.priorityMode()).isEqualTo(TrainingMode.FALSE_FRIEND_DISCRIM);
        assertThat(recommendation.riskLevel()).isIn(RiskLevel.MEDIUM, RiskLevel.HIGH);
        assertThat(recommendation.pairRecommendations()).isNotEmpty();
        assertThat(recommendation.pairRecommendations().getFirst().recommendedMode()).isEqualTo(TrainingMode.FALSE_FRIEND_DISCRIM);
    }

    @Test
    void shouldPrioritizeSpeedChallengeWhenMostlyCorrectButSlow() {
        TrainingRecommendationEngine.RecommendationContext context = new TrainingRecommendationEngine.RecommendationContext(
                1L,
                10L,
                21L,
                0.21,
                0.74,
                0.86,
                1480L,
                List.of(
                        signal(200L, "COGNATE", 0.28, 0, 3, 3, 0, null),
                        signal(201L, "PARTIAL_COGNATE", 0.34, 0, 2, 2, 0, null)
                )
        );

        TrainingRecommendationEngine.TrainingRecommendation recommendation = engine.recommend(context);

        assertThat(recommendation.priorityMode()).isEqualTo(TrainingMode.SPEED_CHALLENGE);
        assertThat(recommendation.pairRecommendations()).allMatch(pair -> pair.recommendedMode() == TrainingMode.SPEED_CHALLENGE
                || pair.recommendedMode() == TrainingMode.COGNATE_BOOST);
    }

    @Test
    void shouldPrioritizeContextFixWhenContextSignalIsWeak() {
        TrainingRecommendationEngine.RecommendationContext context = new TrainingRecommendationEngine.RecommendationContext(
                1L,
                10L,
                22L,
                0.31,
                0.29,
                0.71,
                1120L,
                List.of(
                        signal(300L, "FALSE_FRIEND", 0.55, 1, 1, 0, 0, "CONTEXT_IGNORED", 2, true),
                        signal(301L, "PARTIAL_COGNATE", 0.42, 1, 1, 0, 0, "CONTEXT_IGNORED", 1, true)
                )
        );

        TrainingRecommendationEngine.TrainingRecommendation recommendation = engine.recommend(context);

        assertThat(recommendation.priorityMode()).isEqualTo(TrainingMode.CONTEXT_FIX);
        assertThat(recommendation.pairRecommendations()).anyMatch(pair -> pair.recommendedMode() == TrainingMode.CONTEXT_FIX);
    }

    private TrainingRecommendationEngine.PairSignal signal(
            Long lexicalPairId,
            String lexicalPairType,
            double diagnosisRiskScore,
            long errorCount,
            long correctCount,
            long slowCorrectCount,
            long repeatWrongCount,
            String dominantErrorType
    ) {
        return signal(lexicalPairId, lexicalPairType, diagnosisRiskScore, errorCount, correctCount, slowCorrectCount, repeatWrongCount, dominantErrorType, 0, false);
    }

    private TrainingRecommendationEngine.PairSignal signal(
            Long lexicalPairId,
            String lexicalPairType,
            double diagnosisRiskScore,
            long errorCount,
            long correctCount,
            long slowCorrectCount,
            long repeatWrongCount,
            String dominantErrorType,
            long contextIgnoredCount,
            boolean hasContextExample
    ) {
        return new TrainingRecommendationEngine.PairSignal(
                lexicalPairId,
                "pair-" + lexicalPairId,
                "pair-" + lexicalPairId,
                "词义" + lexicalPairId,
                lexicalPairType,
                "COGNATE".equals(lexicalPairType) ? 0.92 : 0.32,
                "FALSE_FRIEND".equals(lexicalPairType) || "ORTHOGRAPHIC_SIMILAR".equals(lexicalPairType) ? 0.91 : 0.18,
                hasContextExample ? "HIGH" : "LOW",
                3,
                diagnosisRiskScore,
                errorCount,
                correctCount,
                slowCorrectCount,
                errorCount + correctCount,
                repeatWrongCount,
                1260,
                dominantErrorType,
                "FALSE_FRIEND_CONFUSION".equals(dominantErrorType) ? errorCount : 0,
                "ORTHOGRAPHIC_INTERFERENCE".equals(dominantErrorType) ? errorCount : 0,
                contextIgnoredCount,
                "UNDER_TRANSFER".equals(dominantErrorType) ? errorCount : 0,
                hasContextExample
        );
    }
}
