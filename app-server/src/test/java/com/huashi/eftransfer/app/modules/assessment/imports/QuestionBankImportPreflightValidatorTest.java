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
    void enforcesFf4ChoiceRolesSourcePagesAndReviewStatus() {
        QuestionBankImportPackageRequest request = new QuestionBankImportPackageRequest(
                validPackage().questionnaire(),
                List.of(new QuestionBankImportPackageRequest.SectionRow(
                        "FF4_WORD_MEANING", "题型一", null, null, 1, true)),
                List.of(new QuestionBankImportPackageRequest.ItemRow(
                        "Q1", "FF4_WORD_MEANING", "SINGLE_CHOICE", "verger", null, List.of("A"), "explanation",
                        true, true, BigDecimal.ONE, "FALSE_FRIEND", "WORD", "FF4_WORD_MEANING", "verger", null,
                        20, 100, null, null, null, null, "LEXICAL_REVIEW_PENDING", "PEDAGOGIC_REVIEW_PENDING")),
                List.of(
                        new QuestionBankImportPackageRequest.OptionRow("Q1", "A", "壁炉", false, null, "DISTRACTOR"),
                        new QuestionBankImportPackageRequest.OptionRow("Q1", "B", "果酱", false, null, "DISTRACTOR"),
                        new QuestionBankImportPackageRequest.OptionRow("Q1", "C", "果园", false, null, "TRANSFER"),
                        new QuestionBankImportPackageRequest.OptionRow("Q1", "D", "果园", true, null, "DISTRACTOR")
                ));

        QuestionBankImportPreflightValidator.Result result = validator.validate(request, Map.of());

        assertThat(result.status()).isEqualTo("PREFLIGHT_FAILED");
        assertThat(result.issues()).extracting(QuestionBankImportPreflightValidator.Issue::code)
                .contains("INVALID_OPTION_ROLES", "ANSWER_ROLE_MISMATCH");
    }

    @Test
    void enforcesSpellingDistanceAndMorphologyExclusion() {
        QuestionBankImportPackageRequest request = new QuestionBankImportPackageRequest(
                validPackage().questionnaire(),
                List.of(new QuestionBankImportPackageRequest.SectionRow(
                        "FF4_SPELLING", "题型四", null, null, 1, true)),
                List.of(new QuestionBankImportPackageRequest.ItemRow(
                        "Q1", "FF4_SPELLING", "SPELLING", "果园 ______", null, List.of("verger"), "explanation",
                        true, true, BigDecimal.ONE, "FALSE_FRIEND", "WORD", "FF4_SPELLING", "verger", null,
                        0, 0, null, 0, 0, true, "LEXICAL_REVIEW_PENDING", "PEDAGOGIC_REVIEW_PENDING")),
                List.of());

        QuestionBankImportPreflightValidator.Result result = validator.validate(request, Map.of());

        assertThat(result.status()).isEqualTo("PREFLIGHT_FAILED");
        assertThat(result.issues()).extracting(QuestionBankImportPreflightValidator.Issue::code)
                .contains("MISSING_SOURCE_PAGES", "SPELLING_DISTANCE_OUT_OF_RANGE", "MORPHOLOGY_ONLY_EXCLUDED");
    }

    @Test
    void rejectsFf4TrueFalseItemsThatAreNotAllFalse() {
        QuestionBankImportPackageRequest request = new QuestionBankImportPackageRequest(
                validPackage().questionnaire(),
                List.of(new QuestionBankImportPackageRequest.SectionRow(
                        "FF4_TRUE_FALSE", "题型三", null, null, 1, true)),
                List.of(new QuestionBankImportPackageRequest.ItemRow(
                        "Q1", "FF4_TRUE_FALSE", "TRUE_FALSE",
                        "法语 verger 表示“果园”。", null, List.of("V"), "explanation",
                        true, true, BigDecimal.ONE, "FALSE_FRIEND", "WORD", "FF4_TRUE_FALSE_TRANSFER",
                        "verger", null, 20, 100, null, null, null, null,
                        "LEXICAL_REVIEW_PENDING", "PEDAGOGIC_REVIEW_PENDING")),
                List.of(
                        new QuestionBankImportPackageRequest.OptionRow("Q1", "V", "正确", false, null, "DISTRACTOR"),
                        new QuestionBankImportPackageRequest.OptionRow("Q1", "F", "错误", true, null, "CORRECT")
                ));

        QuestionBankImportPreflightValidator.Result result = validator.validate(request, Map.of());

        assertThat(result.status()).isEqualTo("PREFLIGHT_FAILED");
        assertThat(result.issues()).extracting(QuestionBankImportPreflightValidator.Issue::code)
                .contains("TRUE_FALSE_DESIGN_VIOLATION");
    }

    @Test
    void pendingFf4ReviewStatusesKeepImportBehindReviewGate() {
        QuestionBankImportPackageRequest request = ff4TrueFalsePackage(
                "LEXICAL_REVIEW_PENDING", "PEDAGOGIC_REVIEW_PENDING");

        QuestionBankImportPreflightValidator.Result result = validator.validate(request, Map.of());

        assertThat(result.status()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.issues()).extracting(QuestionBankImportPreflightValidator.Issue::code)
                .contains("LEXICAL_REVIEW_PENDING", "PEDAGOGIC_REVIEW_PENDING");
    }

    @Test
    void approvedFf4ReviewStatusesCanPassAfterHumanReview() {
        QuestionBankImportPreflightValidator.Result result = validator.validate(
                ff4TrueFalsePackage("APPROVED", "APPROVED"), Map.of());

        assertThat(result.status()).isEqualTo("READY");
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void ff4ConstructCannotOmitMandatoryMetadata() {
        QuestionBankImportPackageRequest source = ff4TrueFalsePackage(null, null);

        QuestionBankImportPreflightValidator.Result result = validator.validate(source, Map.of());

        assertThat(result.status()).isEqualTo("PREFLIGHT_FAILED");
        assertThat(result.issues()).extracting(QuestionBankImportPreflightValidator.Issue::code)
                .contains("MISSING_LEXICAL_REVIEW_STATUS", "MISSING_PEDAGOGIC_REVIEW_STATUS");
    }

    private QuestionBankImportPackageRequest ff4TrueFalsePackage(String lexical, String pedagogic) {
        return new QuestionBankImportPackageRequest(
                validPackage().questionnaire(),
                List.of(new QuestionBankImportPackageRequest.SectionRow(
                        "FF4_TRUE_FALSE", "题型三", null, null, 1, true)),
                List.of(new QuestionBankImportPackageRequest.ItemRow(
                        "Q1", "FF4_TRUE_FALSE", "TRUE_FALSE", "法语 verger 表示“果园”。", null,
                        List.of("F"), "explanation", true, true, BigDecimal.ONE, "FALSE_FRIEND", "WORD",
                        "FF4_TRUE_FALSE_TRANSFER", "verger", null, 20, 100, null, null, null, false,
                        lexical, pedagogic)),
                List.of(
                        new QuestionBankImportPackageRequest.OptionRow("Q1", "V", "正确", false, null, "DISTRACTOR"),
                        new QuestionBankImportPackageRequest.OptionRow("Q1", "F", "错误", true, null, "CORRECT")
                ));
    }

    @Test
    void rejectsDuplicateTargetWordsAcrossItems() {
        QuestionBankImportPackageRequest source = validPackage();
        QuestionBankImportPackageRequest.ItemRow second = new QuestionBankImportPackageRequest.ItemRow(
                "Q2", "S1", "SINGLE_CHOICE", "Second stem", null, List.of("A"), "Explanation",
                true, true, BigDecimal.ONE, "COGNATE", "WORD", "LEXICAL_TRANSFER", "description", null);
        QuestionBankImportPackageRequest request = new QuestionBankImportPackageRequest(
                source.questionnaire(), source.sections(), List.of(source.items().getFirst(), second),
                List.of(source.options().getFirst(),
                        new QuestionBankImportPackageRequest.OptionRow("Q2", "A", "Alpha", true, null)));

        QuestionBankImportPreflightValidator.Result result = validator.validate(request, Map.of());

        assertThat(result.issues()).extracting(QuestionBankImportPreflightValidator.Issue::code)
                .contains("DUPLICATE_TARGET_WORD");
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
