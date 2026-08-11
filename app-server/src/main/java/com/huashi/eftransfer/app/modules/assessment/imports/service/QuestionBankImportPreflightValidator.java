package com.huashi.eftransfer.app.modules.assessment.imports.service;

import com.huashi.eftransfer.app.modules.assessment.imports.dto.QuestionBankImportPackageRequest;
import com.huashi.eftransfer.shared.enums.AssessmentQuestionType;
import com.huashi.eftransfer.shared.enums.ConstructCode;
import com.huashi.eftransfer.shared.enums.ContextLevel;
import com.huashi.eftransfer.shared.enums.TransferCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class QuestionBankImportPreflightValidator {

    public Result validate(QuestionBankImportPackageRequest request, Map<String, ExistingQuestion> existingByCode) {
        List<Issue> issues = new ArrayList<>();
        Set<String> sectionCodes = new HashSet<>();
        for (QuestionBankImportPackageRequest.SectionRow section : request.sections()) {
            String code = normalize(section.sectionCode());
            if (!sectionCodes.add(code)) {
                issues.add(error("DUPLICATE_SECTION_CODE", code, "Section code is duplicated"));
            }
        }

        Map<String, List<QuestionBankImportPackageRequest.OptionRow>> optionsByItem = new HashMap<>();
        for (QuestionBankImportPackageRequest.OptionRow option : request.options()) {
            optionsByItem.computeIfAbsent(normalize(option.itemCode()), ignored -> new ArrayList<>()).add(option);
        }

        Set<String> itemCodes = new HashSet<>();
        Set<String> targetWords = new HashSet<>();
        int scoredItemCount = 0;
        for (QuestionBankImportPackageRequest.ItemRow item : request.items()) {
            String itemCode = normalize(item.itemCode());
            if (!itemCodes.add(itemCode)) {
                issues.add(error("DUPLICATE_ITEM_CODE", itemCode, "Item code is duplicated"));
            }
            if (item.targetWord() != null && !item.targetWord().isBlank()
                    && !targetWords.add(normalize(item.targetWord()))) {
                issues.add(error("DUPLICATE_TARGET_WORD", itemCode,
                        "Target word is reused across items: " + item.targetWord()));
            }
            if (!sectionCodes.contains(normalize(item.sectionCode()))) {
                issues.add(error("UNKNOWN_SECTION", itemCode, "Item references an unknown section"));
            }
            boolean knownQuestionType = known(() -> AssessmentQuestionType.fromCode(item.questionType()));
            if (!knownQuestionType) {
                issues.add(error("UNKNOWN_QUESTION_TYPE", itemCode, "Unknown question type: " + item.questionType()));
            }
            validateOptionalLabel(item.transferCategory(), itemCode, "UNKNOWN_TRANSFER_CATEGORY", issues,
                    value -> TransferCategory.fromCode(value));
            validateOptionalLabel(item.contextLevel(), itemCode, "UNKNOWN_CONTEXT_LEVEL", issues,
                    value -> ContextLevel.fromCode(value));
            validateOptionalLabel(item.constructCode(), itemCode, "UNKNOWN_CONSTRUCT_CODE", issues,
                    value -> ConstructCode.fromCode(value));

            if (item.scored()) {
                scoredItemCount++;
                if (item.correctAnswers().stream().allMatch(value -> value == null || value.isBlank())) {
                    issues.add(error("MISSING_CORRECT_ANSWER", itemCode, "Scored item has no correct answer"));
                }
            }

            List<QuestionBankImportPackageRequest.OptionRow> options = optionsByItem.getOrDefault(itemCode, List.of());
            Set<String> optionCodes = new HashSet<>();
            for (QuestionBankImportPackageRequest.OptionRow option : options) {
                if (!optionCodes.add(normalize(option.optionCode()))) {
                    issues.add(error("OPTION_CONFLICT", itemCode,
                            "Option code is duplicated for item: " + option.optionCode()));
                }
            }
            long markedCorrect = options.stream().filter(QuestionBankImportPackageRequest.OptionRow::correct).count();
            if (knownQuestionType && isSingleAnswer(item.questionType()) && markedCorrect > 1) {
                issues.add(review("SUSPECTED_MULTIPLE_ANSWERS", itemCode,
                        "Single-answer item has more than one option marked correct"));
            }

            validateFf4Metadata(item, options, issues);

            ExistingQuestion existing = existingByCode == null ? null : existingByCode.get(itemCode);
            if (existing != null && !same(existing.stemText(), item.stemText())) {
                issues.add(review("STEM_DIFFERENCE", itemCode, "Stem differs from the latest bank version"));
            }
            if (existing != null && !same(existing.explanationText(), item.explanationText())) {
                issues.add(review("EXPLANATION_DIFFERENCE", itemCode,
                        "Explanation differs from the latest bank version"));
            }
        }

        String status = issues.stream().anyMatch(issue -> issue.severity().equals("ERROR"))
                ? "PREFLIGHT_FAILED"
                : issues.isEmpty() ? "READY" : "REVIEW_REQUIRED";
        return new Result(status, List.copyOf(issues), scoredItemCount);
    }

    /** FF4 V2/V3 specific checks. FF4 metadata is mandatory and review status
     * controls whether the import remains behind the human-review gate. */
    private void validateFf4Metadata(
            QuestionBankImportPackageRequest.ItemRow item,
            List<QuestionBankImportPackageRequest.OptionRow> options,
            List<Issue> issues
    ) {
        String itemCode = normalize(item.itemCode());
        String constructCode = item.constructCode() == null ? ""
                : item.constructCode().trim().toUpperCase(Locale.ROOT);
        boolean ff4 = constructCode.startsWith("FF4_");
        if (!ff4) {
            return;
        }
        if (item.tem4PdfPage() == null || item.tem4PdfPage() < 1
                || item.falseFriendsPdfPage() == null || item.falseFriendsPdfPage() < 1) {
            issues.add(error("MISSING_SOURCE_PAGES", itemCode,
                    "FF4 items require tem4PdfPage and falseFriendsPdfPage"));
        }
        validateReviewStatus(item.lexicalReviewStatus(), "LEXICAL", itemCode, issues);
        validateReviewStatus(item.pedagogicReviewStatus(), "PEDAGOGIC", itemCode, issues);
        if (constructCode.startsWith("FF4_")) {
            String type = constructCode;
            if (type.equals("FF4_WORD_MEANING") || type.equals("FF4_SENTENCE_SYNONYM")) {
                long correct = options.stream().filter(o -> "CORRECT".equalsIgnoreCase(roleOf(o.role()))).count();
                long transfer = options.stream().filter(o -> "TRANSFER".equalsIgnoreCase(roleOf(o.role()))).count();
                long distractors = options.stream().filter(o -> "DISTRACTOR".equalsIgnoreCase(roleOf(o.role()))).count();
                if (correct != 1 || transfer != 1 || distractors != 2) {
                    issues.add(error("INVALID_OPTION_ROLES", itemCode,
                            "FF4 choice items need exactly 1 CORRECT, 1 TRANSFER, 2 DISTRACTOR options"));
                }
                long markedCorrect = options.stream().filter(QuestionBankImportPackageRequest.OptionRow::correct).count();
                if (markedCorrect != 1 || item.correctAnswers() == null || item.correctAnswers().size() != 1) {
                    issues.add(error("INVALID_CORRECT_ANSWER", itemCode,
                            "FF4 choice items need exactly one correct answer"));
                } else {
                    String roleCorrectKey = options.stream()
                            .filter(o -> "CORRECT".equalsIgnoreCase(roleOf(o.role())) && o.correct())
                            .map(QuestionBankImportPackageRequest.OptionRow::optionCode)
                            .findFirst().orElse(null);
                    if (roleCorrectKey == null || !item.correctAnswers().get(0).equalsIgnoreCase(roleCorrectKey)) {
                        issues.add(error("ANSWER_ROLE_MISMATCH", itemCode,
                                "correctAnswers must match the CORRECT-role option key"));
                    }
                }
                if (type.equals("FF4_SENTENCE_SYNONYM")
                        && !"SOURCE_VERIFIED".equalsIgnoreCase(item.exampleSentenceStatus())) {
                    issues.add(error("SENTENCE_EVIDENCE_MISSING", itemCode,
                            "FF4 sentence items require a source-verified TEM4 example sentence"));
                }
            } else if (type.equals("FF4_TRUE_FALSE_TRANSFER")) {
                if (item.correctAnswers() == null || item.correctAnswers().size() != 1
                        || !"F".equalsIgnoreCase(item.correctAnswers().get(0))) {
                    issues.add(error("TRUE_FALSE_DESIGN_VIOLATION", itemCode,
                            "FF4 true/false items must be F under the current all-false design"));
                }
            } else if (type.equals("FF4_SPELLING")) {
                int distance = Math.min(emptyToMax(item.spellingRawEditDistance()),
                        emptyToMax(item.spellingAccentFoldedEditDistance()));
                if (distance < 1 || distance > 4) {
                    issues.add(error("SPELLING_DISTANCE_OUT_OF_RANGE", itemCode,
                            "FF4 spelling distance must be between 1 and 4: " + distance));
                }
                if (Boolean.TRUE.equals(item.morphologyOnly())) {
                    issues.add(error("MORPHOLOGY_ONLY_EXCLUDED", itemCode,
                            "FF4 spelling items must not use morphology-only word pairs"));
                }
                if (item.correctAnswers() == null || item.correctAnswers().isEmpty()) {
                    issues.add(error("SPELLING_ANSWER_MISSING", itemCode,
                            "FF4 spelling items require the target word as answer"));
                }
            }
        }
    }

    private void validateReviewStatus(String status, String kind, String itemCode, List<Issue> issues) {
        if (status == null || status.isBlank()) {
            issues.add(error("MISSING_" + kind + "_REVIEW_STATUS", itemCode,
                    "FF4 items require " + kind.toLowerCase(Locale.ROOT) + "ReviewStatus"));
            return;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("APPROVED".equals(normalized)) {
            return;
        }
        if ((kind + "_REVIEW_PENDING").equals(normalized) || "REVIEW_PENDING".equals(normalized)) {
            issues.add(review(kind + "_REVIEW_PENDING", itemCode,
                    kind + " human review is still pending"));
            return;
        }
        issues.add(error("INVALID_" + kind + "_REVIEW_STATUS", itemCode,
                "Unsupported " + kind.toLowerCase(Locale.ROOT) + " review status: " + status));
    }

    private static String roleOf(String role) {
        return role == null ? "" : role.trim();
    }

    private static int emptyToMax(Integer value) {
        return value == null ? Integer.MAX_VALUE : value;
    }

    private void validateOptionalLabel(
            String value,
            String itemCode,
            String issueCode,
            List<Issue> issues,
            LabelParser parser
    ) {
        if (value != null && !value.isBlank() && !known(() -> parser.parse(value))) {
            issues.add(error(issueCode, itemCode, "Unknown label: " + value));
        }
    }

    private boolean isSingleAnswer(String type) {
        AssessmentQuestionType parsed = AssessmentQuestionType.fromCode(type);
        return parsed == AssessmentQuestionType.SINGLE_CHOICE
                || parsed == AssessmentQuestionType.TRUE_FALSE
                || parsed == AssessmentQuestionType.TRUE_FALSE_WITH_JUSTIFICATION;
    }

    private boolean known(Runnable parser) {
        try {
            parser.run();
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private static boolean same(String left, String right) {
        return Objects.equals(trimToNull(left), trimToNull(right));
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static Issue error(String code, String itemCode, String message) {
        return new Issue(code, "ERROR", itemCode, message);
    }

    private static Issue review(String code, String itemCode, String message) {
        return new Issue(code, "REVIEW_REQUIRED", itemCode, message);
    }

    @FunctionalInterface
    private interface LabelParser {
        Object parse(String value);
    }

    public record ExistingQuestion(String stemText, String explanationText) {
    }

    public record Issue(String code, String severity, String itemCode, String message) {
    }

    public record Result(String status, List<Issue> issues, int scoredItemCount) {
    }
}
