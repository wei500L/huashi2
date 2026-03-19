package com.huashi.eftransfer.app.modules.diagnosis;

import com.huashi.eftransfer.app.modules.diagnosis.service.DiagnosisScoringPolicy;
import com.huashi.eftransfer.app.modules.diagnosis.service.RuleBasedDiagnosisScoringPolicy;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisOptionPayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisScoringProfilePayload;
import com.huashi.eftransfer.shared.enums.DiagnosisErrorType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosisErrorClassificationTest {

    private final RuleBasedDiagnosisScoringPolicy scoringPolicy = new RuleBasedDiagnosisScoringPolicy();

    @Test
    void shouldIdentifyFalseFriendConfusion() {
        DiagnosisScoringPolicy.ItemEvaluation evaluation = scoringPolicy.evaluate(
                definition("FALSE_FRIEND", "REACTION_TIME", "LOW", false, "semantic_mismatch"),
                new DiagnosisScoringPolicy.SubmittedAnswer(true, null, 1300, 300)
        );
        assertThat(evaluation.errorType()).isEqualTo(DiagnosisErrorType.FALSE_FRIEND_CONFUSION);
    }

    @Test
    void shouldIdentifyContextIgnored() {
        DiagnosisScoringPolicy.ItemEvaluation evaluation = scoringPolicy.evaluate(
                semanticDefinition("FALSE_FRIEND", "HIGH", false, "currently_correct"),
                new DiagnosisScoringPolicy.SubmittedAnswer(null, "actually_trap", 1500, 400)
        );
        assertThat(evaluation.errorType()).isEqualTo(DiagnosisErrorType.CONTEXT_IGNORED);
    }

    @Test
    void shouldIdentifyOverTransfer() {
        DiagnosisScoringPolicy.ItemEvaluation evaluation = scoringPolicy.evaluate(
                definition("PARTIAL_COGNATE", "REACTION_TIME", "MEDIUM", false, "semantic_mismatch"),
                new DiagnosisScoringPolicy.SubmittedAnswer(true, null, 1180, 220)
        );
        assertThat(evaluation.errorType()).isEqualTo(DiagnosisErrorType.OVER_TRANSFER);
    }

    @Test
    void shouldIdentifyUnderTransfer() {
        DiagnosisScoringPolicy.ItemEvaluation evaluation = scoringPolicy.evaluate(
                definition("COGNATE", "REACTION_TIME", "LOW", true, "semantic_match"),
                new DiagnosisScoringPolicy.SubmittedAnswer(false, null, 900, 160)
        );
        assertThat(evaluation.errorType()).isEqualTo(DiagnosisErrorType.UNDER_TRANSFER);
    }

    @Test
    void shouldIdentifyOrthographicInterference() {
        DiagnosisScoringPolicy.ItemEvaluation evaluation = scoringPolicy.evaluate(
                definition("ORTHOGRAPHIC_SIMILAR", "REACTION_TIME", "MEDIUM", false, "semantic_mismatch"),
                new DiagnosisScoringPolicy.SubmittedAnswer(true, null, 1420, 310)
        );
        assertThat(evaluation.errorType()).isEqualTo(DiagnosisErrorType.ORTHOGRAPHIC_INTERFERENCE);
    }

    @Test
    void shouldIdentifySemanticMisfire() {
        DiagnosisScoringPolicy.ItemEvaluation evaluation = scoringPolicy.evaluate(
                semanticDefinition("PARTIAL_COGNATE", "HIGH", false, "option_b"),
                new DiagnosisScoringPolicy.SubmittedAnswer(null, "option_c", 1250, 240)
        );
        assertThat(evaluation.errorType()).isEqualTo(DiagnosisErrorType.SEMANTIC_MISFIRE);
    }

    private DiagnosisScoringPolicy.ItemDefinition definition(
            String lexicalPairType,
            String taskType,
            String contextSupportLevel,
            boolean expectedSemanticMatch,
            String correctAnswerKey
    ) {
        return new DiagnosisScoringPolicy.ItemDefinition(
                1L,
                lexicalPairType,
                taskType,
                contextSupportLevel,
                expectedSemanticMatch,
                correctAnswerKey,
                List.of(
                        new DiagnosisOptionPayload("semantic_match", "Match", true, false),
                        new DiagnosisOptionPayload("semantic_mismatch", "Mismatch", false, false)
                ),
                new DiagnosisScoringProfilePayload("RULE_V1", 1.0, 1.0, 1500),
                "english",
                "french",
                0.88,
                0.20
        );
    }

    private DiagnosisScoringPolicy.ItemDefinition semanticDefinition(
            String lexicalPairType,
            String contextSupportLevel,
            boolean expectedSemanticMatch,
            String correctAnswerKey
    ) {
        return new DiagnosisScoringPolicy.ItemDefinition(
                1L,
                lexicalPairType,
                "SEMANTIC_JUDGEMENT",
                contextSupportLevel,
                expectedSemanticMatch,
                correctAnswerKey,
                List.of(
                        new DiagnosisOptionPayload("actually_trap", "Actually", true, true),
                        new DiagnosisOptionPayload("currently_correct", "Currently", false, false),
                        new DiagnosisOptionPayload("option_b", "Option B", false, false),
                        new DiagnosisOptionPayload("option_c", "Option C", false, false)
                ),
                new DiagnosisScoringProfilePayload("RULE_V1", 1.0, 1.0, 2000),
                "english",
                "french",
                0.88,
                0.20
        );
    }
}
