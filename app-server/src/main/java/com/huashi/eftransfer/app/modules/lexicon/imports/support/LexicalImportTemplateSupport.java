package com.huashi.eftransfer.app.modules.lexicon.imports.support;

import com.huashi.eftransfer.app.modules.lexicon.dto.LexicalPairExampleRequest;
import com.huashi.eftransfer.app.modules.lexicon.dto.LexicalPairSenseRequest;
import com.huashi.eftransfer.app.modules.lexicon.dto.LexicalPairUpsertRequest;
import com.huashi.eftransfer.app.modules.lexicon.imports.dto.LexicalImportRowUpdateRequest;
import com.huashi.eftransfer.app.modules.lexicon.vo.CsvImportTemplateFieldVO;
import com.huashi.eftransfer.app.modules.lexicon.vo.CsvImportTemplateVO;
import com.huashi.eftransfer.shared.api.ResultCode;
import com.huashi.eftransfer.shared.enums.ContextSupportLevel;
import com.huashi.eftransfer.shared.enums.EmbeddingStatus;
import com.huashi.eftransfer.shared.enums.KnowledgeStatus;
import com.huashi.eftransfer.shared.enums.LexicalPairType;
import com.huashi.eftransfer.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class LexicalImportTemplateSupport {

    private static final List<CsvImportTemplateFieldVO> TEMPLATE_FIELDS = List.of(
            new CsvImportTemplateFieldVO("english_word", true, "English lexical item", "coin"),
            new CsvImportTemplateFieldVO("french_word", true, "French lexical item", "coin"),
            new CsvImportTemplateFieldVO("chinese_gloss", true, "Chinese gloss", "硬币；角落"),
            new CsvImportTemplateFieldVO("lexical_pair_type", true, "cognate / false_friend / partial_cognate / orthographic_similar", "false_friend"),
            new CsvImportTemplateFieldVO("semantic_overlap_score", true, "0.0 - 1.0", "0.10"),
            new CsvImportTemplateFieldVO("false_friend_risk", true, "0.0 - 1.0", "0.92"),
            new CsvImportTemplateFieldVO("default_context_support", true, "low / medium / high", "high"),
            new CsvImportTemplateFieldVO("difficulty_level", true, "1 - 5", "4"),
            new CsvImportTemplateFieldVO("notes", false, "Teacher notes", "High confusion for beginners"),
            new CsvImportTemplateFieldVO("source", false, "Source or textbook", "Teacher Curated"),
            new CsvImportTemplateFieldVO("active", false, "true / false", "true"),
            new CsvImportTemplateFieldVO("tags", false, "Pipe separated tags", "false-friend|high-frequency"),
            new CsvImportTemplateFieldVO("knowledge_status", false, "draft / ready / disabled", "ready"),
            new CsvImportTemplateFieldVO("embedding_status", false, "pending / embedded / failed", "pending"),
            new CsvImportTemplateFieldVO("sense_english_definition", false, "Primary English definition", "a piece of money"),
            new CsvImportTemplateFieldVO("sense_french_definition", false, "Primary French definition", "pièce de monnaie"),
            new CsvImportTemplateFieldVO("sense_chinese_definition", false, "Primary Chinese definition", "硬币"),
            new CsvImportTemplateFieldVO("example_english", false, "Primary English example", "I found a coin on the floor."),
            new CsvImportTemplateFieldVO("example_french", false, "Primary French example", "J'ai trouvé une pièce dans la rue."),
            new CsvImportTemplateFieldVO("example_chinese", false, "Primary Chinese translation", "我在地上捡到一枚硬币。"),
            new CsvImportTemplateFieldVO("example_context_support", false, "low / medium / high", "high")
    );

    private static final Set<String> REQUIRED_HEADERS = TEMPLATE_FIELDS.stream()
            .filter(CsvImportTemplateFieldVO::required)
            .map(CsvImportTemplateFieldVO::fieldName)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    public List<CsvImportTemplateFieldVO> templateFields() {
        return TEMPLATE_FIELDS;
    }

    public List<String> templateHeaders() {
        return TEMPLATE_FIELDS.stream()
                .map(CsvImportTemplateFieldVO::fieldName)
                .toList();
    }

    public CsvImportTemplateVO template() {
        String headerLine = TEMPLATE_FIELDS.stream()
                .map(CsvImportTemplateFieldVO::fieldName)
                .collect(Collectors.joining(","));
        String exampleLine = TEMPLATE_FIELDS.stream()
                .map(CsvImportTemplateFieldVO::example)
                .collect(Collectors.joining(","));
        return new CsvImportTemplateVO(TEMPLATE_FIELDS, headerLine, exampleLine);
    }

    public void validateHeaders(Collection<String> headers) {
        Set<String> headerSet = headers.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> missingHeaders = REQUIRED_HEADERS.stream()
                .filter(requiredHeader -> !headerSet.contains(requiredHeader))
                .toList();
        if (!missingHeaders.isEmpty()) {
            throw new BusinessException(
                    ResultCode.VALIDATION_ERROR,
                    "Missing required CSV headers: " + String.join(", ", missingHeaders),
                    400
            );
        }
    }

    public LexicalImportRowDraft toDraft(Map<String, String> fields) {
        return new LexicalImportRowDraft(
                field(fields, "english_word"),
                field(fields, "french_word"),
                field(fields, "chinese_gloss"),
                field(fields, "lexical_pair_type"),
                field(fields, "semantic_overlap_score"),
                field(fields, "false_friend_risk"),
                field(fields, "default_context_support"),
                field(fields, "difficulty_level"),
                field(fields, "notes"),
                field(fields, "source"),
                field(fields, "active"),
                field(fields, "tags"),
                field(fields, "knowledge_status"),
                field(fields, "embedding_status"),
                field(fields, "sense_english_definition"),
                field(fields, "sense_french_definition"),
                field(fields, "sense_chinese_definition"),
                field(fields, "example_english"),
                field(fields, "example_french"),
                field(fields, "example_chinese"),
                field(fields, "example_context_support")
        );
    }

    public LexicalImportRowDraft toDraft(LexicalImportRowUpdateRequest request) {
        return new LexicalImportRowDraft(
                trimToNull(request.englishWord()),
                trimToNull(request.frenchWord()),
                trimToNull(request.chineseGloss()),
                trimToNull(request.lexicalPairType()),
                trimToNull(request.semanticOverlapScore()),
                trimToNull(request.falseFriendRisk()),
                trimToNull(request.defaultContextSupport()),
                trimToNull(request.difficultyLevel()),
                trimToNull(request.notes()),
                trimToNull(request.source()),
                trimToNull(request.active()),
                trimToNull(request.tags()),
                trimToNull(request.knowledgeStatus()),
                trimToNull(request.embeddingStatus()),
                trimToNull(request.senseEnglishDefinition()),
                trimToNull(request.senseFrenchDefinition()),
                trimToNull(request.senseChineseDefinition()),
                trimToNull(request.exampleEnglish()),
                trimToNull(request.exampleFrench()),
                trimToNull(request.exampleChinese()),
                trimToNull(request.exampleContextSupport())
        );
    }

    public LexicalPairUpsertRequest toUpsertRequest(LexicalImportRowDraft draft) {
        String englishWord = requiredField(draft.englishWord(), "english_word");
        String frenchWord = requiredField(draft.frenchWord(), "french_word");
        String chineseGloss = requiredField(draft.chineseGloss(), "chinese_gloss");
        String lexicalPairType = requiredField(draft.lexicalPairType(), "lexical_pair_type");
        String semanticOverlapScore = requiredField(draft.semanticOverlapScore(), "semantic_overlap_score");
        String falseFriendRisk = requiredField(draft.falseFriendRisk(), "false_friend_risk");
        String defaultContextSupport = requiredField(draft.defaultContextSupport(), "default_context_support");
        String difficultyLevel = requiredField(draft.difficultyLevel(), "difficulty_level");

        return new LexicalPairUpsertRequest(
                englishWord,
                frenchWord,
                chineseGloss,
                lexicalPairType,
                parseDecimal(semanticOverlapScore, "semantic_overlap_score"),
                parseDecimal(falseFriendRisk, "false_friend_risk"),
                defaultContextSupport,
                parseInteger(difficultyLevel, "difficulty_level"),
                trimToNull(draft.notes()),
                trimToNull(draft.source()),
                parseBoolean(draft.active()),
                trimToNull(draft.knowledgeStatus()),
                trimToNull(draft.embeddingStatus()),
                splitPipeSeparatedValues(draft.tags()),
                buildImportSenses(draft)
        );
    }

    private List<LexicalPairSenseRequest> buildImportSenses(LexicalImportRowDraft draft) {
        String senseEnglishDefinition = trimToNull(draft.senseEnglishDefinition());
        String senseFrenchDefinition = trimToNull(draft.senseFrenchDefinition());
        String senseChineseDefinition = trimToNull(draft.senseChineseDefinition());
        boolean hasSense = hasText(senseEnglishDefinition) || hasText(senseFrenchDefinition) || hasText(senseChineseDefinition);

        String exampleEnglish = trimToNull(draft.exampleEnglish());
        String exampleFrench = trimToNull(draft.exampleFrench());
        String exampleChinese = trimToNull(draft.exampleChinese());
        String exampleContextSupport = trimToNull(draft.exampleContextSupport());
        boolean hasExample = hasText(exampleEnglish) || hasText(exampleFrench) || hasText(exampleChinese);

        if (!hasSense && !hasExample) {
            return List.of();
        }
        if (!hasSense) {
            throw new BusinessException(
                    ResultCode.VALIDATION_ERROR,
                    "Sense definition is required when example columns are provided",
                    400
            );
        }

        List<LexicalPairExampleRequest> examples = hasExample
                ? List.of(new LexicalPairExampleRequest(
                1,
                exampleEnglish,
                exampleFrench,
                exampleChinese,
                exampleContextSupport == null ? ContextSupportLevel.MEDIUM.code() : exampleContextSupport,
                null
        ))
                : List.of();

        return List.of(new LexicalPairSenseRequest(
                1,
                senseEnglishDefinition,
                senseFrenchDefinition,
                senseChineseDefinition,
                examples
        ));
    }

    private List<String> splitPipeSeparatedValues(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        for (String item : value.split("\\|")) {
            String trimmed = trimToNull(item);
            if (trimmed == null) {
                continue;
            }
            normalized.putIfAbsent(trimmed.toLowerCase(), trimmed);
        }
        return List.copyOf(normalized.values());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String requiredField(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, fieldName + " is required", 400);
        }
        return trimmed;
    }

    private String field(Map<String, String> fields, String key) {
        return trimToNull(fields.get(key));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal parseDecimal(String value, String fieldName) {
        try {
            BigDecimal decimal = new BigDecimal(value);
            if (decimal.compareTo(BigDecimal.ZERO) < 0 || decimal.compareTo(BigDecimal.ONE) > 0) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, fieldName + " must be between 0 and 1", 400);
            }
            return decimal;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, fieldName + " must be a decimal number", 400);
        }
    }

    private Integer parseInteger(String value, String fieldName) {
        try {
            int integer = Integer.parseInt(value);
            if (integer < 1 || integer > 5) {
                throw new BusinessException(ResultCode.VALIDATION_ERROR, fieldName + " must be between 1 and 5", 400);
            }
            return integer;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ResultCode.VALIDATION_ERROR, fieldName + " must be an integer", 400);
        }
    }

    private Boolean parseBoolean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if ("true".equalsIgnoreCase(value.trim())) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value.trim())) {
            return Boolean.FALSE;
        }
        throw new BusinessException(ResultCode.VALIDATION_ERROR, "active must be true or false", 400);
    }

    @SuppressWarnings("unused")
    private LexicalPairType parseLexicalPairType(String value) {
        return LexicalPairType.fromCode(value.trim());
    }

    @SuppressWarnings("unused")
    private KnowledgeStatus parseKnowledgeStatus(String value) {
        return value == null || value.isBlank() ? KnowledgeStatus.DRAFT : KnowledgeStatus.fromCode(value.trim());
    }

    @SuppressWarnings("unused")
    private EmbeddingStatus parseEmbeddingStatus(String value) {
        return value == null || value.isBlank() ? EmbeddingStatus.PENDING : EmbeddingStatus.fromCode(value.trim());
    }
}
