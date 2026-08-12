package com.huashi.eftransfer.app.modules.practice.support;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Catalog of the student self-practice question bank: the FF4 V2 four-type
 * bank seeded from the Lexi-Bridge production package. Only the four formal
 * sections are exposed to students; BASIC_INFO / consent items never appear.
 */
public final class PracticeSectionCatalog {

    public static final String BANK_CODE = "LEXIBRIDGE_FF4_V2";
    public static final String SECTION_ALL = "ALL";
    public static final int MAX_SPELLING_WRONG_ATTEMPTS = 30;

    public record SectionMeta(String code, String title, String description, List<String> constructCodes) {
    }

    public static final List<SectionMeta> SECTIONS = List.of(
            new SectionMeta(
                    "FF4_WORD_MEANING",
                    "Partie 1 - 词义单选（假朋友核心义判断）",
                    "在英语/法语同形词的核心义之间做出选择，识别假朋友的首要含义。",
                    List.of("FF4_WORD_MEANING")
            ),
            new SectionMeta(
                    "FF4_SENTENCE_SELECTION",
                    "Partie 2 - 句子选词（语境近义替换）",
                    "根据句子语境选择正确的法语词，练习语境中的近义替换。",
                    List.of("FF4_SENTENCE_SYNONYM")
            ),
            new SectionMeta(
                    "FF4_TRUE_FALSE",
                    "Partie 3 - 判断正误（迁移义判定）",
                    "判断英语单词的法语对应义是否正确，辨析迁移义与干扰义。",
                    List.of("FF4_TRUE_FALSE_TRANSFER")
            ),
            new SectionMeta(
                    "FF4_SPELLING",
                    "Partie 4 - 单词拼写（形近干扰）",
                    "根据中文释义填写法语单词，首次答错后显示首字母提示。",
                    List.of("FF4_SPELLING")
            )
    );

    private static final Set<String> SECTION_CODES = SECTIONS.stream()
            .map(SectionMeta::code)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

    private static final Map<String, String> SECTION_BY_CONSTRUCT = SECTIONS.stream()
            .flatMap(section -> section.constructCodes().stream()
                    .map(construct -> Map.entry(construct, section.code())))
            .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));

    private PracticeSectionCatalog() {
    }

    public static boolean isPracticeSection(String sectionCode) {
        return sectionCode != null && SECTION_CODES.contains(sectionCode);
    }

    public static SectionMeta metaOf(String sectionCode) {
        return SECTIONS.stream()
                .filter(section -> section.code().equals(sectionCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown practice section: " + sectionCode));
    }

    /**
     * Maps a bank question's construct code back to its practice section.
     * The FF4 V2 bank maps constructs to sections one-to-one, so the section
     * is derived from the construct when no section column exists on the bank
     * version row.
     */
    public static String sectionOfConstruct(String constructCode) {
        if (constructCode == null) {
            return null;
        }
        return SECTION_BY_CONSTRUCT.get(constructCode);
    }

    /**
     * Grades one answer. Choice answers must match the option key set; text
     * answers (SPELLING / FILL_BLANK / SHORT_TEXT) match after accent folding,
     * consistent with the research questionnaire scoring rules.
     */
    public static boolean isCorrect(String questionType, List<String> actual, List<String> expected) {
        String normalizedType = questionType == null ? "" : questionType.toUpperCase(Locale.ROOT);
        boolean textAnswer = normalizedType.contains("FILL_BLANK")
                || normalizedType.contains("SHORT_TEXT")
                || normalizedType.contains("SPELLING");
        Set<String> actualSet = normalized(actual, textAnswer);
        Set<String> expectedSet = normalized(expected, textAnswer);
        if (actualSet.isEmpty() || expectedSet.isEmpty()) {
            return false;
        }
        if (normalizedType.contains("MULTIPLE") || normalizedType.contains("TRUE_FALSE")) {
            return actualSet.equals(expectedSet);
        }
        return actualSet.size() == 1 && expectedSet.size() == 1 && actualSet.equals(expectedSet);
    }

    private static Set<String> normalized(List<String> values, boolean foldText) {
        if (values == null) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = normalizeBlank(value);
            if (normalized == null) {
                continue;
            }
            if (foldText) {
                normalized = normalized.replace('\u2018', '\'').replace('\u2019', '\'')
                        .replace('\u2010', '-').replace('\u2011', '-')
                        .replace('\u2013', '-').replace('\u2014', '-');
                normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}+", "")
                        .replaceAll("\\s+", " ");
            }
            result.add(normalized.toUpperCase(Locale.ROOT));
        }
        return result;
    }

    private static String normalizeBlank(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
