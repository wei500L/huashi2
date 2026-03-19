package com.huashi.eftransfer.app.modules.training.service;

import com.huashi.eftransfer.shared.enums.LexicalPairType;
import com.huashi.eftransfer.shared.enums.RiskLevel;
import com.huashi.eftransfer.shared.enums.TrainingMode;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RuleBasedTrainingRecommendationEngine implements TrainingRecommendationEngine {

    private static final int SLOW_REACTION_THRESHOLD_MS = 1200;

    private static final Map<TrainingMode, List<String>> TARGET_METRICS = Map.of(
            TrainingMode.FALSE_FRIEND_DISCRIM, List.of("降低 false friend 错误率", "识别语义陷阱", "稳定高风险词对"),
            TrainingMode.CONTEXT_FIX, List.of("提升语境敏感度", "先读语境再定词义", "修复高语境误判"),
            TrainingMode.SPEED_CHALLENGE, List.of("缩短平均反应时", "保持正确率下的快速识别", "强化自动化加工"),
            TrainingMode.COGNATE_BOOST, List.of("稳定正迁移线索", "提升同源词提取速度", "减少保守型漏判")
    );

    @Override
    public TrainingRecommendation recommend(RecommendationContext context) {
        if (context.pairSignals().isEmpty()) {
            return new TrainingRecommendation(
                    TrainingMode.COGNATE_BOOST,
                    "最近一次诊断缺少可训练的词对信号，先从正迁移强化开始。",
                    1,
                    RiskLevel.LOW,
                    0,
                    TARGET_METRICS.get(TrainingMode.COGNATE_BOOST),
                    List.of()
            );
        }

        TrainingMode priorityMode = determinePriorityMode(context);
        List<PairRecommendation> pairRecommendations = context.pairSignals().stream()
                .map(signal -> recommendPair(signal, priorityMode))
                .sorted(Comparator.comparingDouble(PairRecommendation::priorityScore).reversed()
                        .thenComparing(PairRecommendation::lexicalPairId))
                .limit(12)
                .toList();

        int estimatedTrainingVolume = pairRecommendations.stream()
                .mapToInt(PairRecommendation::expectedExposures)
                .sum();
        int recommendedDifficulty = pairRecommendations.isEmpty()
                ? 1
                : clamp((int) Math.round(pairRecommendations.stream()
                .mapToInt(PairRecommendation::recommendedDifficulty)
                .average()
                .orElse(1.0)), 1, 5);
        RiskLevel riskLevel = pairRecommendations.stream()
                .map(PairRecommendation::riskLevel)
                .max(Comparator.comparingInt(this::riskRank))
                .orElse(RiskLevel.LOW);

        return new TrainingRecommendation(
                priorityMode,
                buildPlanReason(context, priorityMode),
                recommendedDifficulty,
                riskLevel,
                estimatedTrainingVolume,
                TARGET_METRICS.getOrDefault(priorityMode, List.of()),
                pairRecommendations
        );
    }

    private TrainingMode determinePriorityMode(RecommendationContext context) {
        long falseFriendErrors = context.pairSignals().stream()
                .mapToLong(signal -> signal.falseFriendErrorCount() + signal.orthographicErrorCount())
                .sum();
        long contextErrors = context.pairSignals().stream()
                .mapToLong(PairSignal::contextIgnoredCount)
                .sum();
        long slowCorrect = context.pairSignals().stream()
                .mapToLong(PairSignal::slowCorrectCount)
                .sum();
        long repeatedWrong = context.pairSignals().stream()
                .mapToLong(PairSignal::repeatWrongCount)
                .sum();
        long underTransfer = context.pairSignals().stream()
                .mapToLong(PairSignal::underTransferCount)
                .sum();
        long totalExposures = context.pairSignals().stream()
                .mapToLong(PairSignal::totalExposures)
                .sum();

        double falseFriendScore = context.negativeTransferRisk()
                + ratio(falseFriendErrors + repeatedWrong, totalExposures) * 0.9;
        double contextScore = (1 - context.contextSensitivity())
                + ratio(contextErrors, totalExposures) * 0.9;
        double speedScore = (context.overallAccuracy() >= 0.65 ? 0.35 : 0.10)
                + ratio(slowCorrect, totalExposures) * 0.9
                + (context.averageReactionTime() >= SLOW_REACTION_THRESHOLD_MS ? 0.35 : 0.0);
        double cognateScore = ratio(underTransfer, totalExposures) * 0.8
                + (0.70 - context.overallAccuracy()) * 0.4;

        double max = Math.max(Math.max(falseFriendScore, contextScore), Math.max(speedScore, cognateScore));
        if (max == falseFriendScore && falseFriendScore >= 0.55) {
            return TrainingMode.FALSE_FRIEND_DISCRIM;
        }
        if (max == contextScore && contextScore >= 0.55) {
            return TrainingMode.CONTEXT_FIX;
        }
        if (max == speedScore && speedScore >= 0.55) {
            return TrainingMode.SPEED_CHALLENGE;
        }
        return TrainingMode.COGNATE_BOOST;
    }

    private PairRecommendation recommendPair(PairSignal signal, TrainingMode priorityMode) {
        double normalizedRisk = clamp(signal.diagnosisRiskScore()
                + signal.repeatWrongCount() * 0.15
                + signal.errorCount() * 0.08
                + signal.slowCorrectCount() * 0.05
                + signal.falseFriendRisk() * 0.10, 0, 1);
        RiskLevel riskLevel = resolveRiskLevel(normalizedRisk);
        TrainingMode mode = determinePairMode(signal, priorityMode);
        int difficulty = resolveDifficulty(signal, mode, riskLevel);
        int expectedExposures = switch (riskLevel) {
            case HIGH, CRITICAL -> 4;
            case MEDIUM -> 3;
            case LOW -> 2;
        };
        double priorityScore = round4(
                normalizedRisk * 70
                        + signal.errorCount() * 8
                        + signal.repeatWrongCount() * 6
                        + signal.slowCorrectCount() * 5
                        + signal.falseFriendRisk() * 10
        );
        String reason = buildPairReason(signal, mode);
        String targetContextSupport = mode == TrainingMode.CONTEXT_FIX
                ? (signal.hasContextExample() ? "HIGH" : "MEDIUM")
                : signal.defaultContextSupport();

        return new PairRecommendation(
                signal.lexicalPairId(),
                mode,
                difficulty,
                riskLevel,
                priorityScore,
                reason,
                signal.dominantErrorType(),
                targetContextSupport,
                expectedExposures
        );
    }

    private TrainingMode determinePairMode(PairSignal signal, TrainingMode priorityMode) {
        LexicalPairType pairType = LexicalPairType.fromCode(signal.lexicalPairType());
        if (signal.repeatWrongCount() >= 2
                || signal.falseFriendErrorCount() > 0
                || signal.orthographicErrorCount() > 0
                || pairType == LexicalPairType.FALSE_FRIEND
                || pairType == LexicalPairType.ORTHOGRAPHIC_SIMILAR) {
            return TrainingMode.FALSE_FRIEND_DISCRIM;
        }
        if (signal.contextIgnoredCount() > 0 || (priorityMode == TrainingMode.CONTEXT_FIX && signal.hasContextExample())) {
            return TrainingMode.CONTEXT_FIX;
        }
        if (signal.slowCorrectCount() > 0 && signal.errorCount() == 0) {
            return TrainingMode.SPEED_CHALLENGE;
        }
        if (signal.underTransferCount() > 0
                || pairType == LexicalPairType.COGNATE
                || pairType == LexicalPairType.PARTIAL_COGNATE) {
            return TrainingMode.COGNATE_BOOST;
        }
        return priorityMode;
    }

    private int resolveDifficulty(PairSignal signal, TrainingMode mode, RiskLevel riskLevel) {
        int difficulty = signal.difficultyLevel();
        if (riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL) {
            difficulty += 1;
        }
        if (signal.repeatWrongCount() >= 2) {
            difficulty += 1;
        }
        if (mode == TrainingMode.SPEED_CHALLENGE && signal.errorCount() == 0) {
            difficulty -= 1;
        }
        return clamp(difficulty, 1, 5);
    }

    private String buildPlanReason(RecommendationContext context, TrainingMode mode) {
        return switch (mode) {
            case FALSE_FRIEND_DISCRIM -> "最近一次诊断显示你在 false friend / 形近干扰上的错误占主导，高风险词对需要先做专项辨析。";
            case CONTEXT_FIX -> "最近一次诊断显示你对语境线索的利用不足，训练应优先修复高语境下的词义判断。";
            case SPEED_CHALLENGE -> "你在最近一次诊断里多数题目能够答对，但反应偏慢，下一步应优先压缩识别反应时。";
            case COGNATE_BOOST -> context.overallAccuracy() < 0.70
                    ? "当前推荐先稳定可利用的正迁移线索，减少本可利用却被保守放弃的词对。"
                    : "当前整体风险可控，先通过正迁移强化把稳定词对转化成更快的自动化识别。";
        };
    }

    private String buildPairReason(PairSignal signal, TrainingMode mode) {
        return switch (mode) {
            case FALSE_FRIEND_DISCRIM -> {
                String error = signal.dominantErrorType() == null ? "false friend 混淆" : signal.dominantErrorType();
                yield "该词对近期以 " + error + " 为主，且重复错误较多，需要优先做负迁移纠偏。";
            }
            case CONTEXT_FIX -> "该词对在语境介入时更容易失误，建议通过句内修正训练重新锁定义项。";
            case SPEED_CHALLENGE -> "该词对当前多为答对但反应偏慢，适合转入快速识别训练压缩决策时间。";
            case COGNATE_BOOST -> "该词对具备较高正迁移价值，但稳定度还不够，需要继续强化可直接提取的对应关系。";
        };
    }

    private RiskLevel resolveRiskLevel(double value) {
        if (value >= 0.75) {
            return RiskLevel.HIGH;
        }
        if (value >= 0.45) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    private int riskRank(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case LOW -> 1;
            case MEDIUM -> 2;
            case HIGH -> 3;
            case CRITICAL -> 4;
        };
    }

    private double ratio(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return (double) numerator / denominator;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
