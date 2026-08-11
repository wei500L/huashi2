package com.huashi.eftransfer.app.modules.assessment.imports;

import com.huashi.eftransfer.app.modules.assessment.imports.dto.QuestionBankImportPackageRequest;
import com.huashi.eftransfer.app.modules.assessment.imports.service.QuestionBankImportPreflightValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionBankImportPreflightValidatorTest {

    private final QuestionBankImportPreflightValidator validator = new QuestionBankImportPreflightValidator();

    @Test
    void acceptsACompletePackage() {
        QuestionBankImportPreflightValidator.Result result = validator.validate(validPackage(), Map.of());

        assertThat(result.status()).isEqualTo("READY");
        assertThat(result.issues()).isEmpty();
        assertThat(result.scoredItemCount()).isEqualTo(1);
    }

    @Test
    void rejectsDuplicateItemsMissingAnswersOptionConflictsAndUnknownLabels() {
        QuestionBankImportPackageRequest source = validPackage();
        QuestionBankImportPackageRequest.ItemRow bad = new QuestionBankImportPackageRequest.ItemRow(
                "Q1", "S1", "ALIEN_TYPE", "Different stem", null, List.of(), "Different explanation",
                true, true, BigDecimal.ONE, "ALIEN_TRANSFER", "ALIEN_CONTEXT", "ALIEN_CONSTRUCT", null, null);
        QuestionBankImportPackageRequest.OptionRow conflicting = new QuestionBankImportPackageRequest.OptionRow(
                "Q1", "A", "Other A", false, null);
        QuestionBankImportPackageRequest request = new QuestionBankImportPackageRequest(
                source.questionnaire(), source.sections(), List.of(source.items().getFirst(), bad),
                List.of(source.options().getFirst(), conflicting));

        QuestionBankImportPreflightValidator.Result result = validator.validate(request, Map.of());

        assertThat(result.status()).isEqualTo("PREFLIGHT_FAILED");
        assertThat(result.issues()).extracting(QuestionBankImportPreflightValidator.Issue::code)
                .contains("DUPLICATE_ITEM_CODE", "MISSING_CORRECT_ANSWER", "OPTION_CONFLICT",
                        "UNKNOWN_QUESTION_TYPE", "UNKNOWN_TRANSFER_CATEGORY", "UNKNOWN_CONTEXT_LEVEL",
                        "UNKNOWN_CONSTRUCT_CODE");
    }

    @Test
    void requiresReviewForSourceDifferencesAndSuspectedMultipleAnswers() {
        QuestionBankImportPackageRequest source = validPackage();
        QuestionBankImportPackageRequest request = new QuestionBankImportPackageRequest(
                source.questionnaire(), source.sections(), source.items(),
                List.of(
                        new QuestionBankImportPackageRequest.OptionRow("Q1", "A", "Alpha", true, "A explanation"),
                        new QuestionBankImportPackageRequest.OptionRow("Q1", "B", "Beta", true, "B explanation")
                ));
        QuestionBankImportPreflightValidator.ExistingQuestion existing =
                new QuestionBankImportPreflightValidator.ExistingQuestion("Old stem", "Old explanation");

        QuestionBankImportPreflightValidator.Result result = validator.validate(request, Map.of("Q1", existing));

        assertThat(result.status()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.issues()).extracting(QuestionBankImportPreflightValidator.Issue::code)
                .contains("STEM_DIFFERENCE", "EXPLANATION_DIFFERENCE", "SUSPECTED_MULTIPLE_ANSWERS");
    }

    @Test
    void acceptsEarlierFieldConditionsAndUnambiguousStructuredPresentation() {
        QuestionBankImportPackageRequest request = packageWithItems(List.of(
                new QuestionBankImportPackageRequest.ItemRow(
                        "PROFILE-BRANCH", "S1", "SINGLE_CHOICE", "Branch", null, List.of(), null,
                        true, false, BigDecimal.ONE, null, null, null, null, null, null),
                new QuestionBankImportPackageRequest.ItemRow(
                        "Q1", "S1", "SINGLE_CHOICE", "un problème important", null, List.of("A"), "Explanation",
                        true, true, BigDecimal.ONE, "COGNATE", "WORD", "LEXICAL_TRANSFER", "important",
                        "{\"fieldCode\":\"PROFILE-BRANCH\",\"operator\":\"EQ\",\"value\":\"A\"}",
                        "{\"emphasis\":[{\"text\":\"important\",\"bold\":true,\"underline\":true}]}")),
                List.of(
                        new QuestionBankImportPackageRequest.OptionRow("PROFILE-BRANCH", "A", "A", false, null),
                        new QuestionBankImportPackageRequest.OptionRow("Q1", "A", "Alpha", true, null)));

        QuestionBankImportPreflightValidator.Result result = validator.validate(request, Map.of());

        assertThat(result.status()).isEqualTo("READY");
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void rejectsForwardConditionsAndAmbiguousPresentationTargets() {
        QuestionBankImportPackageRequest request = packageWithItems(List.of(
                new QuestionBankImportPackageRequest.ItemRow(
                        "Q1", "S1", "SINGLE_CHOICE", "important puis important", null, List.of("A"), "Explanation",
                        true, true, BigDecimal.ONE, "COGNATE", "WORD", "LEXICAL_TRANSFER", "important",
                        "{\"fieldCode\":\"LATER-FIELD\",\"operator\":\"EQ\",\"value\":\"A\"}",
                        "{\"emphasis\":[{\"text\":\"important\",\"bold\":true,\"underline\":true}]}"),
                new QuestionBankImportPackageRequest.ItemRow(
                        "LATER-FIELD", "S1", "SHORT_TEXT", "Later", null, List.of(), null,
                        false, false, BigDecimal.ONE, null, null, null, null, null, null)),
                List.of(new QuestionBankImportPackageRequest.OptionRow("Q1", "A", "Alpha", true, null)));

        QuestionBankImportPreflightValidator.Result result = validator.validate(request, Map.of());

        assertThat(result.status()).isEqualTo("PREFLIGHT_FAILED");
        assertThat(result.issues()).extracting(QuestionBankImportPreflightValidator.Issue::code)
                .contains("INVALID_DISPLAY_CONDITION_REFERENCE", "PRESENTATION_TARGET_AMBIGUOUS");
    }

    private static QuestionBankImportPackageRequest packageWithItems(
            List<QuestionBankImportPackageRequest.ItemRow> items,
            List<QuestionBankImportPackageRequest.OptionRow> options
    ) {
        return new QuestionBankImportPackageRequest(
                new QuestionBankImportPackageRequest.QuestionnaireRow(
                        "LEXIBRIDGE_RESEARCH_V2", "Lexi-Bridge V2", "Research questionnaire", 40,
                        "SCORING_V1", "assessment-analysis/v2"),
                List.of(new QuestionBankImportPackageRequest.SectionRow(
                        "S1", "Vocabulary", null, null, 1, true)),
                items,
                options);
    }

    static QuestionBankImportPackageRequest validPackage() {
        return new QuestionBankImportPackageRequest(
                new QuestionBankImportPackageRequest.QuestionnaireRow(
                        "LEXIBRIDGE_RESEARCH_V1", "Lexi-Bridge", "Research questionnaire", 40,
                        "SCORING_V1", "assessment-analysis/v1"),
                List.of(new QuestionBankImportPackageRequest.SectionRow(
                        "S1", "Vocabulary", null, null, 1, true)),
                List.of(new QuestionBankImportPackageRequest.ItemRow(
                        "Q1", "S1", "SINGLE_CHOICE", "Stem", null, List.of("A"), "Explanation",
                        true, true, BigDecimal.ONE, "COGNATE", "WORD", "LEXICAL_TRANSFER", "description", null)),
                List.of(
                        new QuestionBankImportPackageRequest.OptionRow("Q1", "A", "Alpha", true, "A explanation"),
                        new QuestionBankImportPackageRequest.OptionRow("Q1", "B", "Beta", false, "B explanation")
                ));
    }
}
