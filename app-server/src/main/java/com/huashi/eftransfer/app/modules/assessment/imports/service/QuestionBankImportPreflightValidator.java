package com.huashi.eftransfer.app.modules.assessment.imports.service;

import com.huashi.eftransfer.app.modules.assessment.imports.dto.QuestionBankImportPackageRequest;
import com.huashi.eftransfer.shared.enums.AssessmentQuestionType;
import com.huashi.eftransfer.shared.enums.ConstructCode;
import com.huashi.eftransfer.shared.enums.ContextLevel;
import com.huashi.eftransfer.shared.enums.TransferCategory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

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

    private final ObjectMapper objectMapper;

    public QuestionBankImportPreflightValidator() {
        this(new ObjectMapper());
    }

    public QuestionBankImportPreflightValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

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
        Set<String> precedingItemCodes = new HashSet<>();
        int scoredItemCount = 0;
        for (QuestionBankImportPackageRequest.ItemRow item : request.items()) {
            String itemCode = normalize(item.itemCode());
            if (!itemCodes.add(itemCode)) {
                issues.add(error("DUPLICATE_ITEM_CODE", itemCode, "Item code is duplicated"));
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
            validateDisplayCondition(item, itemCode, precedingItemCodes, issues);
            validatePresentation(item, itemCode, issues);

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

            ExistingQuestion existing = existingByCode == null ? null : existingByCode.get(itemCode);
            if (existing != null && !same(existing.stemText(), item.stemText())) {
                issues.add(review("STEM_DIFFERENCE", itemCode, "Stem differs from the latest bank version"));
            }
            if (existing != null && !same(existing.explanationText(), item.explanationText())) {
                issues.add(review("EXPLANATION_DIFFERENCE", itemCode,
                        "Explanation differs from the latest bank version"));
            }
            precedingItemCodes.add(itemCode);
        }

        String status = issues.stream().anyMatch(issue -> issue.severity().equals("ERROR"))
                ? "PREFLIGHT_FAILED"
                : issues.isEmpty() ? "READY" : "REVIEW_REQUIRED";
        return new Result(status, List.copyOf(issues), scoredItemCount);
    }

    private void validateDisplayCondition(
            QuestionBankImportPackageRequest.ItemRow item,
            String itemCode,
            Set<String> precedingItemCodes,
            List<Issue> issues
    ) {
        if (item.displayConditionJson() == null || item.displayConditionJson().isBlank()) return;
        try {
            JsonNode condition = objectMapper.readTree(item.displayConditionJson());
            String fieldCode = normalize(condition.path("fieldCode").asString());
            String operator = condition.path("operator").asString();
            String value = condition.path("value").asString();
            if (fieldCode.isBlank() || !"EQ".equalsIgnoreCase(operator) || value.isBlank()) {
                issues.add(error("INVALID_DISPLAY_CONDITION", itemCode,
                        "Display condition must contain fieldCode, operator EQ and value"));
            } else if (!precedingItemCodes.contains(fieldCode)) {
                issues.add(error("INVALID_DISPLAY_CONDITION_REFERENCE", itemCode,
                        "Display condition must reference an earlier item: " + fieldCode));
            }
        } catch (Exception exception) {
            issues.add(error("INVALID_DISPLAY_CONDITION", itemCode, "Display condition is not valid JSON"));
        }
    }

    private void validatePresentation(
            QuestionBankImportPackageRequest.ItemRow item,
            String itemCode,
            List<Issue> issues
    ) {
        if (item.presentationJson() == null || item.presentationJson().isBlank()) return;
        try {
            JsonNode presentation = objectMapper.readTree(item.presentationJson());
            JsonNode emphasis = presentation.path("emphasis");
            if (!emphasis.isArray()) {
                issues.add(error("INVALID_PRESENTATION", itemCode, "presentation.emphasis must be an array"));
                return;
            }
            String stem = item.stemText() == null ? "" : item.stemText();
            for (JsonNode mark : emphasis) {
                String text = mark.path("text").asString();
                int occurrence = mark.path("occurrence").isMissingNode() || mark.path("occurrence").isNull()
                        ? 0 : mark.path("occurrence").asInt();
                int count = countOccurrences(stem, text);
                if (text.isBlank() || count == 0) {
                    issues.add(error("PRESENTATION_TARGET_NOT_FOUND", itemCode,
                            "Presentation emphasis text was not found in the stem: " + text));
                } else if (occurrence <= 0 && count > 1) {
                    issues.add(error("PRESENTATION_TARGET_AMBIGUOUS", itemCode,
                            "Presentation emphasis text occurs more than once; set occurrence"));
                } else if (occurrence > count) {
                    issues.add(error("PRESENTATION_OCCURRENCE_OUT_OF_RANGE", itemCode,
                            "Presentation occurrence exceeds matching text count"));
                }
            }
        } catch (Exception exception) {
            issues.add(error("INVALID_PRESENTATION", itemCode, "Presentation is not valid JSON"));
        }
    }

    private int countOccurrences(String source, String target) {
        if (source == null || target == null || target.isBlank()) return 0;
        int count = 0;
        int from = 0;
        while ((from = source.indexOf(target, from)) >= 0) {
            count++;
            from += target.length();
        }
        return count;
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
