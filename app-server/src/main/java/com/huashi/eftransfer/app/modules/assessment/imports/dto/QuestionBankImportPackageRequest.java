package com.huashi.eftransfer.app.modules.assessment.imports.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

/** JSON equivalent of the Questionnaire, Sections, Items and Options workbook sheets. */
public record QuestionBankImportPackageRequest(
        @JsonProperty("Questionnaire") @JsonAlias("questionnaire") @NotNull @Valid QuestionnaireRow questionnaire,
        @JsonProperty("Sections") @JsonAlias("sections") @NotEmpty @Valid List<SectionRow> sections,
        @JsonProperty("Items") @JsonAlias("items") @NotEmpty @Valid List<ItemRow> items,
        @JsonProperty("Options") @JsonAlias("options") @NotNull @Valid List<OptionRow> options
) {
    public QuestionBankImportPackageRequest {
        sections = sections == null ? List.of() : List.copyOf(sections);
        items = items == null ? List.of() : List.copyOf(items);
        options = options == null ? List.of() : List.copyOf(options);
    }

    public record QuestionnaireRow(
            @NotBlank String code,
            @NotBlank String title,
            String description,
            @NotNull Integer durationMinutes,
            @NotBlank String scoringVersion,
            @NotBlank String aiPromptVersion
    ) {
    }

    public record SectionRow(
            @NotBlank String sectionCode,
            @NotBlank String title,
            String description,
            String sharedMaterial,
            @NotNull Integer sortOrder,
            boolean formalSection
    ) {
    }

    public record ItemRow(
            @NotBlank String itemCode,
            @NotBlank String sectionCode,
            @NotBlank String questionType,
            String stemText,
            String promptText,
            List<String> correctAnswers,
            String explanationText,
            boolean requiredAnswer,
            boolean scored,
            BigDecimal weight,
            String transferCategory,
            String contextLevel,
            String constructCode,
            String targetWord,
            String displayConditionJson,
            String presentationJson
    ) {
        public ItemRow(
                String itemCode,
                String sectionCode,
                String questionType,
                String stemText,
                String promptText,
                List<String> correctAnswers,
                String explanationText,
                boolean requiredAnswer,
                boolean scored,
                BigDecimal weight,
                String transferCategory,
                String contextLevel,
                String constructCode,
                String targetWord,
                String displayConditionJson
        ) {
            this(itemCode, sectionCode, questionType, stemText, promptText, correctAnswers, explanationText,
                    requiredAnswer, scored, weight, transferCategory, contextLevel, constructCode, targetWord,
                    displayConditionJson, null);
        }

        public ItemRow {
            correctAnswers = correctAnswers == null ? List.of() : List.copyOf(correctAnswers);
            weight = weight == null ? BigDecimal.ONE : weight;
        }
    }

    public record OptionRow(
            @NotBlank String itemCode,
            @NotBlank String optionCode,
            @NotBlank String optionText,
            boolean correct,
            String explanation
    ) {
    }
}
