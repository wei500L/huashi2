package com.huashi.eftransfer.app.common.security;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class JwtSecretValidator {

    private static final int MIN_SECRET_LENGTH = 32;
    private static final double MIN_BITS_PER_CHAR = 3.0d;
    private static final double MIN_TOTAL_BITS = 128.0d;
    private static final List<String> PLACEHOLDER_MARKERS = List.of(
            "replace-me",
            "replace_with",
            "changeme",
            "change-me",
            "placeholder",
            "example",
            "sample"
    );
    private static final List<String> PREDICTABLE_SEQUENCES = List.of(
            "01234567",
            "12345678",
            "87654321",
            "98765432",
            "abcdefgh",
            "hgfedcba",
            "qwerty",
            "password",
            "letmein",
            "asdfgh"
    );

    private JwtSecretValidator() {
    }

    static void validate(String secret, String description) {
        if (!StringUtils.hasText(secret) || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(description + " must contain at least 32 characters");
        }

        String normalized = secret.trim().toLowerCase(Locale.ROOT);
        if (looksLikePlaceholder(normalized)) {
            throw new IllegalStateException(description + " must be a randomly generated high-entropy value");
        }
        if (hasRepeatedPattern(secret)) {
            throw new IllegalStateException(description + " must not reuse a repeated character pattern");
        }
        if (containsPredictableSequence(normalized)) {
            throw new IllegalStateException(description + " must not contain predictable keyboard or ordered sequences");
        }
        if (!hasSufficientEntropy(secret)) {
            throw new IllegalStateException(description + " must be a randomly generated high-entropy value");
        }
    }

    private static boolean looksLikePlaceholder(String normalized) {
        if (!StringUtils.hasText(normalized)) {
            return true;
        }
        return PLACEHOLDER_MARKERS.stream().anyMatch(normalized::contains);
    }

    private static boolean hasRepeatedPattern(String secret) {
        int length = secret.length();
        for (int patternLength = 1; patternLength <= length / 2; patternLength++) {
            if (length % patternLength != 0) {
                continue;
            }

            String pattern = secret.substring(0, patternLength);
            if (pattern.repeat(length / patternLength).equals(secret)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsPredictableSequence(String normalized) {
        String collapsed = collapseAdjacentDuplicates(normalized);
        return PREDICTABLE_SEQUENCES.stream().anyMatch(sequence ->
                normalized.contains(sequence) || collapsed.contains(sequence)
        );
    }

    private static String collapseAdjacentDuplicates(String value) {
        StringBuilder collapsed = new StringBuilder(value.length());
        char previous = 0;
        boolean hasPrevious = false;
        for (char current : value.toCharArray()) {
            if (!hasPrevious || current != previous) {
                collapsed.append(current);
                previous = current;
                hasPrevious = true;
            }
        }
        return collapsed.toString();
    }

    private static boolean hasSufficientEntropy(String secret) {
        double bitsPerCharacter = shannonEntropy(secret);
        return bitsPerCharacter >= MIN_BITS_PER_CHAR && bitsPerCharacter * secret.length() >= MIN_TOTAL_BITS;
    }

    private static double shannonEntropy(String secret) {
        int length = secret.length();
        Map<Integer, Long> frequencies = secret.chars()
                .boxed()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        double entropy = 0.0d;
        for (long frequency : frequencies.values()) {
            double probability = (double) frequency / length;
            entropy -= probability * (Math.log(probability) / Math.log(2));
        }
        return entropy;
    }
}
