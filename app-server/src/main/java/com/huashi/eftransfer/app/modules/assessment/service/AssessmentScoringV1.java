package com.huashi.eftransfer.app.modules.assessment.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Deterministic research questionnaire scoring and metric calculations. */
public final class AssessmentScoringV1 {

    public static final String VERSION = "SCORING_V1";

    private AssessmentScoringV1() {
    }

    public static Result score(List<Question> questions, Map<Integer, Response> responses) {
        if (questions == null || questions.isEmpty()) {
            return new Result(0, 0, null, Map.of(), null, null, null, null, List.of());
        }
        double weightedEarned = 0;
        double weightedMaximum = 0;
        int correctCount = 0;
        Map<String, Accuracy> dimensions = new LinkedHashMap<>();
        Map<String, Accuracy> transferCategories = new LinkedHashMap<>();
        Map<String, Accuracy> contextLevels = new LinkedHashMap<>();
        List<Long> reactionTimes = new ArrayList<>();
        List<String> qualityFlags = new ArrayList<>();
        for (Question question : questions) {
            if (question == null) {
                continue;
            }
            int max = Math.max(0, question.maxScore());
            double weight = question.weight() == null || question.weight() <= 0 ? 1d : question.weight();
            weightedMaximum += max * weight;
            Response response = responses == null ? null : responses.get(question.order());
            boolean answered = response != null && response.answers() != null && !response.answers().isEmpty();
            boolean correct = answered && matches(question.questionType(), response.answers(), question.correctAnswers());
            double awarded = correct ? max : 0d;
            weightedEarned += awarded * weight;
            if (correct) {
                correctCount++;
            }
            String dimension = blankToNull(question.dimension());
            if (dimension != null) {
                Accuracy current = dimensions.getOrDefault(dimension, new Accuracy(0, 0));
                dimensions.put(dimension, new Accuracy(current.denominator() + 1, current.numerator() + (correct ? 1 : 0)));
            }
            accumulate(transferCategories, blankToNull(question.transferCategory()), correct);
            accumulate(contextLevels, blankToNull(question.contextLevel()), correct);
            if (response != null && response.reactionTimeMs() != null && response.reactionTimeMs() >= 0) {
                reactionTimes.add(response.reactionTimeMs());
                if (response.reactionTimeMs() < 300) {
                    qualityFlags.add("FAST_ITEM");
                }
            }
        }
        Double percentile = weightedMaximum == 0 ? null : weightedEarned / weightedMaximum * 100d;
        ReactionTimeSummary reaction = reactionTimes.isEmpty() ? null : ReactionTimeSummary.from(reactionTimes);
        if (reaction != null && reaction.totalEffectiveSeconds() < 600) {
            qualityFlags.add("SHORT_TOTAL_DURATION");
        }
        Double cognateAdvantage = differencePoints(transferCategories.get("COGNATE"), transferCategories.get("FRENCH_CONTROL"));
        Double falseFriendInterference = differencePoints(transferCategories.get("FRENCH_CONTROL"), transferCategories.get("FALSE_FRIEND"));
        Accuracy lowContext = merge(contextLevels.get("WORD"), contextLevels.get("PHRASE"));
        Accuracy highContext = merge(contextLevels.get("SENTENCE"), contextLevels.get("CLOZE"), contextLevels.get("READING"));
        Double contextRepair = differencePoints(highContext, lowContext);
        return new Result(correctCount, questions.size(), percentile, dimensions,
                cognateAdvantage, falseFriendInterference, contextRepair, reaction,
                List.copyOf(new LinkedHashSet<>(qualityFlags)));
    }

    private static void accumulate(Map<String, Accuracy> groups, String key, boolean correct) {
        if (key == null) {
            return;
        }
        String normalizedKey = key.toUpperCase(Locale.ROOT);
        Accuracy current = groups.getOrDefault(normalizedKey, new Accuracy(0, 0));
        groups.put(normalizedKey, new Accuracy(current.denominator() + 1, current.numerator() + (correct ? 1 : 0)));
    }

    private static Accuracy merge(Accuracy... values) {
        int denominator = 0;
        int numerator = 0;
        for (Accuracy value : values) {
            if (value != null) {
                denominator += value.denominator();
                numerator += value.numerator();
            }
        }
        return denominator == 0 ? null : new Accuracy(denominator, numerator);
    }

    private static Double differencePoints(Accuracy left, Accuracy right) {
        if (left == null || right == null || left.denominator() == 0 || right.denominator() == 0) {
            return null;
        }
        return (left.ratio() - right.ratio()) * 100d;
    }

    private static boolean matches(String type, List<String> actual, List<String> expected) {
        Set<String> actualSet = normalized(actual);
        Set<String> expectedSet = normalized(expected);
        if (actualSet.isEmpty() || expectedSet.isEmpty()) {
            return false;
        }
        String normalizedType = type == null ? "" : type.toUpperCase(Locale.ROOT);
        if (normalizedType.contains("MULTIPLE") || normalizedType.contains("TRUE_FALSE")) {
            return actualSet.equals(expectedSet);
        }
        return actualSet.size() == 1 && expectedSet.size() == 1 && actualSet.equals(expectedSet);
    }

    private static Set<String> normalized(List<String> values) {
        if (values == null) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = blankToNull(value);
            if (normalized != null) {
                result.add(normalized.toUpperCase(Locale.ROOT));
            }
        }
        return result;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record Question(int order, String questionType, int maxScore, Double weight,
                           String dimension, String contextLevel, String transferCategory, List<String> correctAnswers) {
        public Question {
            correctAnswers = correctAnswers == null ? List.of() : List.copyOf(correctAnswers);
        }
    }

    public record Response(List<String> answers, Long reactionTimeMs, String justificationText) {
        public Response {
            answers = answers == null ? List.of() : List.copyOf(answers);
        }
    }

    public record Accuracy(int denominator, int numerator) {
        public Double ratio() {
            return denominator == 0 ? null : numerator / (double) denominator;
        }
    }

    public record ReactionTimeSummary(long medianMs, long firstQuartileMs, long thirdQuartileMs,
                                      int sampleCount, long totalEffectiveSeconds, List<Long> values) {
        static ReactionTimeSummary from(List<Long> values) {
            List<Long> sorted = values.stream().filter(Objects::nonNull).sorted().toList();
            return new ReactionTimeSummary(percentile(sorted, .5), percentile(sorted, .25), percentile(sorted, .75),
                    sorted.size(), sorted.stream().mapToLong(value -> value).sum() / 1000, sorted);
        }

        private static long percentile(List<Long> sorted, double p) {
            if (sorted.isEmpty()) {
                return 0;
            }
            int index = (int) Math.ceil(p * sorted.size()) - 1;
            return sorted.get(Math.max(0, Math.min(sorted.size() - 1, index)));
        }
    }

    public record Result(int correctCount, int questionCount, Double percentage, Map<String, Accuracy> dimensions,
                         Double cognateAdvantagePoints, Double falseFriendInterferencePoints,
                         Double contextRepairPoints, ReactionTimeSummary reactionTime,
                         List<String> qualityFlags) {
        public Result {
            dimensions = dimensions == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(dimensions));
            qualityFlags = qualityFlags == null ? List.of() : List.copyOf(qualityFlags);
        }
    }
}
