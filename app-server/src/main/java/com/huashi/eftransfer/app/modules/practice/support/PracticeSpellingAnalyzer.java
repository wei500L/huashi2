package com.huashi.eftransfer.app.modules.practice.support;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Classifies what kind of spelling mistake a student made, so tutoring can
 * give targeted feedback (missing accents, letter substitutions, extra or
 * missing letters, or a distant guess).
 */
public final class PracticeSpellingAnalyzer {

    public static final String PATTERN_EXACT = "EXACT";
    public static final String PATTERN_ACCENT_ORTHOGRAPHY = "ACCENT_ORTHOGRAPHY";
    public static final String PATTERN_REPLACED_LETTER = "REPLACED_LETTER";
    public static final String PATTERN_MISSING_LETTER = "MISSING_LETTER";
    public static final String PATTERN_EXTRA_LETTER = "EXTRA_LETTER";
    public static final String PATTERN_CLOSE = "CLOSE";
    public static final String PATTERN_DISTANT = "DISTANT";

    private PracticeSpellingAnalyzer() {
    }

    public static String analyze(String candidate, List<String> expected) {
        if (candidate == null || expected == null || expected.isEmpty()) {
            return PATTERN_DISTANT;
        }
        String trimmed = candidate.trim();
        if (trimmed.isEmpty()) {
            return PATTERN_DISTANT;
        }
        String target = expected.get(0);
        if (target == null || target.isBlank()) {
            return PATTERN_DISTANT;
        }
        if (trimmed.equals(target)) {
            return PATTERN_EXACT;
        }
        String foldedCandidate = fold(trimmed);
        String foldedTarget = fold(target);
        if (foldedCandidate.equals(foldedTarget)) {
            return PATTERN_ACCENT_ORTHOGRAPHY;
        }
        int distance = levenshtein(foldedCandidate, foldedTarget);
        if (distance <= 4) {
            if (distance == 1) {
                if (foldedCandidate.length() == foldedTarget.length()) {
                    return PATTERN_REPLACED_LETTER;
                }
                if (foldedCandidate.length() < foldedTarget.length()) {
                    return PATTERN_MISSING_LETTER;
                }
                return PATTERN_EXTRA_LETTER;
            }
            return PATTERN_CLOSE;
        }
        return PATTERN_DISTANT;
    }

    private static String fold(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        normalized = normalized.replace('\u2018', '\'').replace('\u2019', '\'')
                .replace('\u2010', '-').replace('\u2011', '-')
                .replace('\u2013', '-').replace('\u2014', '-');
        return Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static int levenshtein(String left, String right) {
        if (left.isEmpty()) {
            return right.length();
        }
        if (right.isEmpty()) {
            return left.length();
        }
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }
}
