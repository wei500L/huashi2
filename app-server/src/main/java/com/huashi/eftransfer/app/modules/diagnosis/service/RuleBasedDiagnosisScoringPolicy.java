package com.huashi.eftransfer.app.modules.diagnosis.service;

import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisChartPayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisContextPerformance;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisDistributionItem;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisHighRiskLexicalPair;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisLexicalTypePerformance;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisOptionPayload;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisRadarMetric;
import com.huashi.eftransfer.app.modules.diagnosis.support.DiagnosisResponseTimelinePoint;
import com.huashi.eftransfer.shared.enums.ContextSupportLevel;
import com.huashi.eftransfer.shared.enums.DiagnosisErrorType;
import com.huashi.eftransfer.shared.enums.DiagnosisTaskType;
import com.huashi.eftransfer.shared.enums.LexicalPairType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RuleBasedDiagnosisScoringPolicy implements DiagnosisScoringPolicy {

    private static final Map<LexicalPairType, Double> POSITIVE_PAIR_WEIGHTS = Map.of(
            LexicalPairType.COGNATE, 1.0,
            LexicalPairType.PARTIAL_COGNATE, 0.9,
            LexicalPairType.FALSE_FRIEND, 0.6,
            LexicalPairType.ORTHOGRAPHIC_SIMILAR, 0.6
    );

    private static final Map<LexicalPairType, Double> DISCRIMINATION_WEIGHTS = Map.of(
            LexicalPairType.FALSE_FRIEND, 0.35,
            LexicalPairType.PARTIAL_COGNATE, 0.25,
            LexicalPairType.ORTHOGRAPHIC_SIMILAR, 0.20,
            LexicalPairType.COGNATE, 0.20
    );

    private static final Map<DiagnosisErrorType, Double> ERROR_SEVERITY_WEIGHTS = Map.of(
            DiagnosisErrorType.FALSE_FRIEND_CONFUSION, 1.0,
            DiagnosisErrorType.CONTEXT_IGNORED, 0.85,
            DiagnosisErrorType.OVER_TRANSFER, 0.80,
            DiagnosisErrorType.ORTHOGRAPHIC_INTERFERENCE, 0.75,
            DiagnosisErrorType.UNDER_TRANSFER, 0.60,
            DiagnosisErrorType.SEMANTIC_MISFIRE, 0.55
    );

    @Override
    public ItemEvaluation evaluate(ItemDefinition definition, SubmittedAnswer answer) {
        DiagnosisTaskType taskType = DiagnosisTaskType.fromCode(definition.taskType());
        String selectedAnswerKey = resolveSelectedAnswerKey(definition.options(), answer, taskType);
        DiagnosisOptionPayload selectedOption = definition.options().stream()
                .filter(option -> option.key().equalsIgnoreCase(selectedAnswerKey))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Selected answer key is not defined in template options"));

        boolean semanticConsistent = Boolean.TRUE.equals(selectedOption.semanticMatch());
        boolean correct = definition.correctAnswerKey().equalsIgnoreCase(selectedAnswerKey);
        DiagnosisErrorType errorType = classifyError(definition, selectedOption, correct, semanticConsistent);

        double speedScore = speedScore(answer.reactionTimeMs(), maxReactionTime(definition));
        double hesitationPenalty = clamp((double) answer.hesitationTimeMs() / Math.max(answer.reactionTimeMs(), 1), 0, 1);
        double itemScore = computeItemScore(definition, correct, speedScore, hesitationPenalty);
        double transferRiskScore = computeTransferRisk(definition, correct, speedScore, hesitationPenalty, errorType);

        return new ItemEvaluation(
                selectedAnswerKey,
                correct,
                semanticConsistent,
                errorType,
                clamp(transferRiskScore, 0, 1),
                clamp(itemScore, 0, 1)
        );
    }

    @Override
    public SummaryAggregation aggregate(List<AnsweredItem> answeredItems) {
        if (answeredItems.isEmpty()) {
            return new SummaryAggregation(0, 0, 0, 0, 0, 0, List.of(), List.of(),
                    new DiagnosisChartPayload(List.of(), List.of(), List.of(), List.of(), List.of(), List.of()));
        }

        double overallAccuracy = answeredItems.stream().filter(AnsweredItem::correct).count() / (double) answeredItems.size();
        long averageReactionTime = Math.round(answeredItems.stream().mapToInt(AnsweredItem::reactionTimeMs).average().orElse(0));

        double positiveTransferScore = computePositiveTransferScore(answeredItems);
        double negativeTransferRisk = computeNegativeTransferRisk(answeredItems);
        double contextSensitivity = computeContextSensitivity(answeredItems);
        double semanticDiscrimination = computeSemanticDiscrimination(answeredItems);

        List<DiagnosisDistributionItem> errorDistribution = computeErrorDistribution(answeredItems);
        List<DiagnosisHighRiskLexicalPair> highRiskLexicalPairs = computeHighRiskLexicalPairs(answeredItems);
        DiagnosisChartPayload chartPayload = buildChartPayload(
                answeredItems,
                positiveTransferScore,
                negativeTransferRisk,
                contextSensitivity,
                semanticDiscrimination,
                errorDistribution,
                highRiskLexicalPairs
        );

        return new SummaryAggregation(
                round4(positiveTransferScore),
                round4(negativeTransferRisk),
                round4(contextSensitivity),
                round4(semanticDiscrimination),
                round4(overallAccuracy),
                averageReactionTime,
                errorDistribution,
                highRiskLexicalPairs,
                chartPayload
        );
    }

    private String resolveSelectedAnswerKey(List<DiagnosisOptionPayload> options, SubmittedAnswer answer, DiagnosisTaskType taskType) {
        if (answer.selectedAnswerKey() != null && !answer.selectedAnswerKey().isBlank()) {
            return answer.selectedAnswerKey();
        }
        if (taskType == DiagnosisTaskType.REACTION_TIME && answer.selectedSemanticMatch() != null) {
            return options.stream()
                    .filter(option -> Objects.equals(option.semanticMatch(), answer.selectedSemanticMatch()))
                    .map(DiagnosisOptionPayload::key)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Reaction time task requires options mapped to semanticMatch"));
        }
        throw new IllegalArgumentException("Answer payload does not contain a valid selection");
    }

    private DiagnosisErrorType classifyError(
            ItemDefinition definition,
            DiagnosisOptionPayload selectedOption,
            boolean correct,
            boolean semanticConsistent
    ) {
        if (correct) {
            return null;
        }

        DiagnosisTaskType taskType = DiagnosisTaskType.fromCode(definition.taskType());
        ContextSupportLevel contextSupportLevel = ContextSupportLevel.fromCode(definition.contextSupportLevel());
        LexicalPairType pairType = LexicalPairType.fromCode(definition.lexicalPairType());

        if (taskType == DiagnosisTaskType.SEMANTIC_JUDGEMENT
                && (contextSupportLevel == ContextSupportLevel.MEDIUM || contextSupportLevel == ContextSupportLevel.HIGH)
                && Boolean.TRUE.equals(selectedOption.ignoreContextTrap())) {
            return DiagnosisErrorType.CONTEXT_IGNORED;
        }
        if (pairType == LexicalPairType.FALSE_FRIEND && !definition.expectedSemanticMatch() && semanticConsistent) {
            return DiagnosisErrorType.FALSE_FRIEND_CONFUSION;
        }
        if (pairType == LexicalPairType.ORTHOGRAPHIC_SIMILAR) {
            return DiagnosisErrorType.ORTHOGRAPHIC_INTERFERENCE;
        }
        if (!definition.expectedSemanticMatch() && semanticConsistent) {
            return DiagnosisErrorType.OVER_TRANSFER;
        }
        if (definition.expectedSemanticMatch() && !semanticConsistent) {
            return DiagnosisErrorType.UNDER_TRANSFER;
        }
        return DiagnosisErrorType.SEMANTIC_MISFIRE;
    }

    private double computeItemScore(ItemDefinition definition, boolean correct, double speedScore, double hesitationPenalty) {
        if (definition.expectedSemanticMatch()) {
            return Math.max(0, (correct ? 1.0 : 0.0) * (0.7 + 0.3 * speedScore) - 0.15 * hesitationPenalty);
        }
        return clamp((correct ? (0.65 + 0.35 * speedScore) : (0.15 * speedScore)) - 0.10 * hesitationPenalty, 0, 1);
    }

    private double computeTransferRisk(
            ItemDefinition definition,
            boolean correct,
            double speedScore,
            double hesitationPenalty,
            DiagnosisErrorType errorType
    ) {
        boolean riskTracked = !definition.expectedSemanticMatch()
                || LexicalPairType.fromCode(definition.lexicalPairType()) == LexicalPairType.FALSE_FRIEND
                || LexicalPairType.fromCode(definition.lexicalPairType()) == LexicalPairType.ORTHOGRAPHIC_SIMILAR;

        double risk = riskTracked
                ? (correct ? 0.25 * (1 - speedScore) + 0.20 * hesitationPenalty : 0.75)
                : clamp((1 - computeItemScore(definition, correct, speedScore, hesitationPenalty)) * 0.35, 0, 1);

        if (errorType == DiagnosisErrorType.FALSE_FRIEND_CONFUSION
                || errorType == DiagnosisErrorType.OVER_TRANSFER
                || errorType == DiagnosisErrorType.ORTHOGRAPHIC_INTERFERENCE) {
            risk += 0.15;
        } else if (errorType == DiagnosisErrorType.CONTEXT_IGNORED) {
            risk += 0.10;
        }
        risk += 0.15 * definition.falseFriendRisk();
        return risk;
    }

    private double computePositiveTransferScore(List<AnsweredItem> answeredItems) {
        List<AnsweredItem> positiveItems = answeredItems.stream()
                .filter(item -> item.definition().expectedSemanticMatch())
                .toList();
        if (positiveItems.isEmpty()) {
            return 0;
        }

        double weightedScore = 0;
        double totalWeight = 0;
        for (AnsweredItem item : positiveItems) {
            double speedScore = speedScore(item.reactionTimeMs(), maxReactionTime(item.definition()));
            double hesitationPenalty = clamp((double) item.hesitationTimeMs() / Math.max(item.reactionTimeMs(), 1), 0, 1);
            double accuracy = item.correct() ? 1.0 : 0.0;
            double itemScore = Math.max(0, accuracy * (0.7 + 0.3 * speedScore) - 0.15 * hesitationPenalty);
            double weight = pairWeight(item.definition());
            weightedScore += itemScore * weight;
            totalWeight += weight;
        }
        return totalWeight == 0 ? 0 : clamp(weightedScore / totalWeight, 0, 1);
    }

    private double computeNegativeTransferRisk(List<AnsweredItem> answeredItems) {
        List<AnsweredItem> riskItems = answeredItems.stream()
                .filter(item -> !item.definition().expectedSemanticMatch()
                        || LexicalPairType.fromCode(item.definition().lexicalPairType()) == LexicalPairType.FALSE_FRIEND
                        || LexicalPairType.fromCode(item.definition().lexicalPairType()) == LexicalPairType.ORTHOGRAPHIC_SIMILAR)
                .toList();
        if (riskItems.isEmpty()) {
            return 0;
        }

        double weightedRisk = 0;
        double totalWeight = 0;
        for (AnsweredItem item : riskItems) {
            double weight = Optional.ofNullable(item.definition().scoringProfile())
                    .map(profile -> Optional.ofNullable(profile.riskAmplifier()).orElse(1.0))
                    .orElse(1.0);
            weightedRisk += item.transferRiskScore() * weight;
            totalWeight += weight;
        }
        return totalWeight == 0 ? 0 : clamp(weightedRisk / totalWeight, 0, 1);
    }

    private double computeContextSensitivity(List<AnsweredItem> answeredItems) {
        List<AnsweredItem> semanticItems = answeredItems.stream()
                .filter(item -> DiagnosisTaskType.fromCode(item.definition().taskType()) == DiagnosisTaskType.SEMANTIC_JUDGEMENT)
                .toList();
        if (semanticItems.isEmpty()) {
            return 0;
        }

        double accLow = accuracyForContext(semanticItems, ContextSupportLevel.LOW);
        double accMedium = accuracyForContext(semanticItems, ContextSupportLevel.MEDIUM);
        double accHigh = accuracyForContext(semanticItems, ContextSupportLevel.HIGH);
        double contextIgnoredRate = semanticItems.stream()
                .filter(item -> item.errorType() == DiagnosisErrorType.CONTEXT_IGNORED)
                .count() / (double) semanticItems.size();
        return clamp(0.5 + 0.5 * (accHigh - accLow) + 0.3 * (accMedium - accLow) - 0.4 * contextIgnoredRate, 0, 1);
    }

    private double computeSemanticDiscrimination(List<AnsweredItem> answeredItems) {
        Map<LexicalPairType, List<AnsweredItem>> grouped = answeredItems.stream()
                .collect(Collectors.groupingBy(item -> LexicalPairType.fromCode(item.definition().lexicalPairType())));

        double weightedAccuracy = 0;
        double totalWeight = 0;
        for (Map.Entry<LexicalPairType, Double> entry : DISCRIMINATION_WEIGHTS.entrySet()) {
            List<AnsweredItem> items = grouped.getOrDefault(entry.getKey(), List.of());
            if (items.isEmpty()) {
                continue;
            }
            double accuracy = items.stream().filter(AnsweredItem::correct).count() / (double) items.size();
            weightedAccuracy += accuracy * entry.getValue();
            totalWeight += entry.getValue();
        }
        return totalWeight == 0 ? 0 : clamp(weightedAccuracy / totalWeight, 0, 1);
    }

    private List<DiagnosisDistributionItem> computeErrorDistribution(List<AnsweredItem> answeredItems) {
        Map<DiagnosisErrorType, Long> counts = Arrays.stream(DiagnosisErrorType.values())
                .collect(Collectors.toMap(Function.identity(), errorType -> 0L, (a, b) -> a, () -> new EnumMap<>(DiagnosisErrorType.class)));

        answeredItems.stream()
                .map(AnsweredItem::errorType)
                .filter(Objects::nonNull)
                .forEach(errorType -> counts.compute(errorType, (key, value) -> value == null ? 1L : value + 1));

        long totalErrors = counts.values().stream().mapToLong(Long::longValue).sum();
        List<DiagnosisDistributionItem> distribution = new ArrayList<>();
        for (DiagnosisErrorType errorType : DiagnosisErrorType.values()) {
            long count = counts.getOrDefault(errorType, 0L);
            distribution.add(new DiagnosisDistributionItem(
                    errorType.code(),
                    errorType.label(),
                    count,
                    totalErrors == 0 ? 0 : round4(count / (double) totalErrors)
            ));
        }
        return distribution;
    }

    private List<DiagnosisHighRiskLexicalPair> computeHighRiskLexicalPairs(List<AnsweredItem> answeredItems) {
        Map<Long, List<AnsweredItem>> grouped = answeredItems.stream()
                .collect(Collectors.groupingBy(item -> item.definition().lexicalPairId(), LinkedHashMap::new, Collectors.toList()));

        return grouped.values().stream()
                .map(this::toHighRiskPair)
                .filter(pair -> pair.errorCount() > 0 || pair.riskScore() >= 0.25)
                .sorted(Comparator.comparingDouble(DiagnosisHighRiskLexicalPair::riskScore).reversed())
                .limit(5)
                .toList();
    }

    private DiagnosisHighRiskLexicalPair toHighRiskPair(List<AnsweredItem> items) {
        AnsweredItem sample = items.getFirst();
        long wrongCount = items.stream().filter(item -> !item.correct()).count();
        double wrongRate = wrongCount / (double) items.size();
        double avgTransferRisk = items.stream().mapToDouble(AnsweredItem::transferRiskScore).average().orElse(0);
        long avgReactionTime = Math.round(items.stream().mapToInt(AnsweredItem::reactionTimeMs).average().orElse(0));
        double normalizedRt = clamp((avgReactionTime - 600.0) / 1400.0, 0, 1);
        DiagnosisErrorType dominantError = items.stream()
                .map(AnsweredItem::errorType)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        double errorSeverityWeight = dominantError == null ? 0.3 : ERROR_SEVERITY_WEIGHTS.getOrDefault(dominantError, 0.3);
        double riskScore = 0.45 * wrongRate + 0.25 * avgTransferRisk + 0.15 * normalizedRt + 0.15 * errorSeverityWeight;

        return new DiagnosisHighRiskLexicalPair(
                sample.definition().lexicalPairId(),
                sample.definition().englishWord(),
                sample.definition().frenchWord(),
                LexicalPairType.fromCode(sample.definition().lexicalPairType()),
                round4(clamp(riskScore, 0, 1)),
                wrongCount,
                avgReactionTime,
                dominantError == null ? null : dominantError.code()
        );
    }

    private DiagnosisChartPayload buildChartPayload(
            List<AnsweredItem> answeredItems,
            double positiveTransferScore,
            double negativeTransferRisk,
            double contextSensitivity,
            double semanticDiscrimination,
            List<DiagnosisDistributionItem> errorDistribution,
            List<DiagnosisHighRiskLexicalPair> highRiskLexicalPairs
    ) {
        List<DiagnosisRadarMetric> radarMetrics = List.of(
                new DiagnosisRadarMetric("positiveTransferScore", "Positive Transfer", round4(positiveTransferScore)),
                new DiagnosisRadarMetric("negativeTransferRisk", "Negative Transfer Risk", round4(negativeTransferRisk)),
                new DiagnosisRadarMetric("contextSensitivity", "Context Sensitivity", round4(contextSensitivity)),
                new DiagnosisRadarMetric("semanticDiscrimination", "Semantic Discrimination", round4(semanticDiscrimination)),
                new DiagnosisRadarMetric(
                        "cognitiveFluency",
                        "Cognitive Fluency",
                        round4(Math.min(1, 1500.0 / Math.max(1, answeredItems.stream().mapToInt(AnsweredItem::reactionTimeMs).average().orElse(1500))))
                )
        );

        List<DiagnosisContextPerformance> contextPerformances = Arrays.stream(ContextSupportLevel.values())
                .map(level -> {
                    List<AnsweredItem> items = answeredItems.stream()
                            .filter(item -> ContextSupportLevel.fromCode(item.definition().contextSupportLevel()) == level)
                            .toList();
                    return new DiagnosisContextPerformance(
                            level,
                            items.isEmpty() ? 0 : round4(items.stream().filter(AnsweredItem::correct).count() / (double) items.size()),
                            items.isEmpty() ? 0 : Math.round(items.stream().mapToInt(AnsweredItem::reactionTimeMs).average().orElse(0)),
                            items.size()
                    );
                })
                .toList();

        List<DiagnosisLexicalTypePerformance> lexicalTypePerformance = Arrays.stream(LexicalPairType.values())
                .map(type -> {
                    List<AnsweredItem> items = answeredItems.stream()
                            .filter(item -> LexicalPairType.fromCode(item.definition().lexicalPairType()) == type)
                            .toList();
                    return new DiagnosisLexicalTypePerformance(
                            type,
                            items.isEmpty() ? 0 : round4(items.stream().filter(AnsweredItem::correct).count() / (double) items.size()),
                            items.isEmpty() ? 0 : Math.round(items.stream().mapToInt(AnsweredItem::reactionTimeMs).average().orElse(0)),
                            items.size()
                    );
                })
                .toList();

        List<DiagnosisResponseTimelinePoint> responseTimeline = answeredItems.stream()
                .sorted(Comparator.comparingInt(AnsweredItem::presentationOrder))
                .map(item -> new DiagnosisResponseTimelinePoint(
                        item.presentationOrder(),
                        item.itemResultId(),
                        DiagnosisTaskType.fromCode(item.definition().taskType()),
                        LexicalPairType.fromCode(item.definition().lexicalPairType()),
                        item.reactionTimeMs(),
                        item.correct(),
                        item.errorType() == null ? null : item.errorType().code()
                ))
                .toList();

        return new DiagnosisChartPayload(
                radarMetrics,
                errorDistribution,
                contextPerformances,
                lexicalTypePerformance,
                highRiskLexicalPairs,
                responseTimeline
        );
    }

    private double accuracyForContext(List<AnsweredItem> items, ContextSupportLevel level) {
        List<AnsweredItem> filtered = items.stream()
                .filter(item -> ContextSupportLevel.fromCode(item.definition().contextSupportLevel()) == level)
                .toList();
        if (filtered.isEmpty()) {
            return 0;
        }
        return filtered.stream().filter(AnsweredItem::correct).count() / (double) filtered.size();
    }

    private double pairWeight(ItemDefinition definition) {
        if (definition.scoringProfile() != null && definition.scoringProfile().pairWeight() != null) {
            return definition.scoringProfile().pairWeight();
        }
        return POSITIVE_PAIR_WEIGHTS.getOrDefault(LexicalPairType.fromCode(definition.lexicalPairType()), 1.0);
    }

    private int maxReactionTime(ItemDefinition definition) {
        if (definition.scoringProfile() != null && definition.scoringProfile().maxReactionTimeMs() != null) {
            return definition.scoringProfile().maxReactionTimeMs();
        }
        return DiagnosisTaskType.fromCode(definition.taskType()) == DiagnosisTaskType.REACTION_TIME ? 1500 : 2000;
    }

    private double speedScore(int reactionTimeMs, int maxReactionTimeMs) {
        if (maxReactionTimeMs <= 400) {
            return 0;
        }
        return clamp((maxReactionTimeMs - reactionTimeMs) / Math.max(400.0, maxReactionTimeMs - 400.0), 0, 1);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
