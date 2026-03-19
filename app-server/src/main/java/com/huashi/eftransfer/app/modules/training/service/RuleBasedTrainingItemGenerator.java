package com.huashi.eftransfer.app.modules.training.service;

import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairExampleEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairSenseEntity;
import com.huashi.eftransfer.app.modules.training.support.TrainingOptionPayload;
import com.huashi.eftransfer.app.modules.training.support.TrainingStimulusPayload;
import com.huashi.eftransfer.shared.enums.ContextSupportLevel;
import com.huashi.eftransfer.shared.enums.LexicalPairType;
import com.huashi.eftransfer.shared.enums.TrainingCognitiveTag;
import com.huashi.eftransfer.shared.enums.TrainingItemType;
import com.huashi.eftransfer.shared.enums.TrainingMode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

@Component
public class RuleBasedTrainingItemGenerator implements TrainingItemGenerator {

    @Override
    public GeneratedItem generate(GenerationContext context) {
        TrainingMode mode = TrainingMode.fromCode(context.planItem().getRecommendedMode());
        return switch (mode) {
            case FALSE_FRIEND_DISCRIM -> generateFalseFriendDiscrimination(context);
            case CONTEXT_FIX -> generateContextRepair(context);
            case SPEED_CHALLENGE -> generateRapidRecognition(context);
            case COGNATE_BOOST -> generateCognateStrengthening(context);
        };
    }

    private GeneratedItem generateFalseFriendDiscrimination(GenerationContext context) {
        ExampleSelection selection = chooseExample(context, true);
        LexicalPairEntity pair = context.lexicalPair();
        String correctMeaning = selection.correctMeaning();
        List<String> distractors = buildDistractors(pair, correctMeaning, selection.selectedSense());
        List<TrainingOptionPayload> options = buildChoiceOptions(correctMeaning, distractors, context, 3);
        return new GeneratedItem(
                TrainingItemType.CHOICE,
                TrainingCognitiveTag.TRAP,
                new TrainingStimulusPayload(
                        "优先压制字形诱导，不要被英语表面形式带偏。",
                        selection.example() == null
                                ? "下面哪一项最能避免把法语 \"" + pair.getFrenchWord() + "\" 误判成英语 \"" + pair.getEnglishWord() + "\" 的含义？"
                                : "在句子中，法语 \"" + pair.getFrenchWord() + "\" 更接近哪种含义？",
                        selection.contextSentence(),
                        "这是一组高风险负迁移词对，重点是分清英语直觉和法语真实义项。",
                        selection.contextSupportLevel()
                ),
                options,
                resolveCorrectAnswerKey(options, correctMeaning)
        );
    }

    private GeneratedItem generateContextRepair(GenerationContext context) {
        ExampleSelection selection = chooseExample(context, true);
        LexicalPairEntity pair = context.lexicalPair();
        String correctMeaning = selection.correctMeaning();
        List<String> distractors = buildDistractors(pair, correctMeaning, selection.selectedSense());
        List<TrainingOptionPayload> options = buildChoiceOptions(correctMeaning, distractors, context, 3);
        return new GeneratedItem(
                TrainingItemType.CHOICE,
                TrainingCognitiveTag.TRAP,
                new TrainingStimulusPayload(
                        "先锁定句内语义，再决定词义，不要只看字面形式。",
                        "结合语境判断，句中的 \"" + pair.getFrenchWord() + "\" 最合理的解释是哪一项？",
                        selection.contextSentence(),
                        "这道题优先训练你利用句内线索修正初始直觉。",
                        selection.contextSupportLevel()
                ),
                options,
                resolveCorrectAnswerKey(options, correctMeaning)
        );
    }

    private GeneratedItem generateRapidRecognition(GenerationContext context) {
        LexicalPairEntity pair = context.lexicalPair();
        boolean directMatch = isDirectMatchPair(pair);
        List<TrainingOptionPayload> options = List.of(
                new TrainingOptionPayload("correct", directMatch ? "可以直接对应" : "不应直接对应"),
                new TrainingOptionPayload("wrong", directMatch ? "不应直接对应" : "可以直接对应")
        );
        return new GeneratedItem(
                TrainingItemType.JUDGMENT,
                directMatch ? TrainingCognitiveTag.OPPORTUNITY : TrainingCognitiveTag.TRAP,
                new TrainingStimulusPayload(
                        "减少停顿，用首个稳定判断完成快速识别。",
                        "快速判断：英语 \"" + pair.getEnglishWord() + "\" 与法语 \"" + pair.getFrenchWord() + "\" 的核心含义现在是否可直接对应？",
                        null,
                        directMatch ? "这是稳定正迁移线索，目标是在不掉准确率的前提下提速。" : "虽然词形接近，但核心语义不能直接对应，先稳住准确率再提速。",
                        pair.getDefaultContextSupport()
                ),
                options,
                "correct"
        );
    }

    private GeneratedItem generateCognateStrengthening(GenerationContext context) {
        LexicalPairEntity pair = context.lexicalPair();
        boolean stablePositiveTransfer = isStablePositiveTransfer(pair);
        List<TrainingOptionPayload> options = List.of(
                new TrainingOptionPayload("correct", stablePositiveTransfer ? "适合作为正迁移线索" : "暂时不适合直接迁移"),
                new TrainingOptionPayload("wrong", stablePositiveTransfer ? "暂时不适合直接迁移" : "适合作为正迁移线索")
        );
        return new GeneratedItem(
                TrainingItemType.JUDGMENT,
                TrainingCognitiveTag.OPPORTUNITY,
                new TrainingStimulusPayload(
                        "先抓住稳定对应关系，把可利用的正迁移线索提取出来。",
                        "在当前学习阶段，\"" + pair.getEnglishWord() + "\" 与 \"" + pair.getFrenchWord() + "\" 是否适合作为正迁移线索？",
                        null,
                        stablePositiveTransfer
                                ? "这组词保持了较稳定的语义对应，可以用来建立更快的正迁移提取。"
                                : "这组词表面接近但稳定度不够，仍需要先控制迁移边界。",
                        pair.getDefaultContextSupport()
                ),
                options,
                "correct"
        );
    }

    private ExampleSelection chooseExample(GenerationContext context, boolean preferHighContext) {
        List<ExampleSelection> candidates = new ArrayList<>();
        for (SenseBundle bundle : context.senseBundles()) {
            LexicalPairSenseEntity sense = bundle.sense();
            List<LexicalPairExampleEntity> orderedExamples = bundle.examples().stream()
                    .sorted(Comparator.comparingInt((LexicalPairExampleEntity example) -> contextWeight(example.getContextSupportLevel(), preferHighContext))
                            .reversed()
                            .thenComparing(LexicalPairExampleEntity::getSortOrder)
                            .thenComparing(LexicalPairExampleEntity::getId))
                    .toList();
            if (orderedExamples.isEmpty()) {
                candidates.add(new ExampleSelection(
                        null,
                        sense,
                        normalizeMeaning(sense.getChineseDefinition(), context.lexicalPair().getChineseGloss()),
                        null,
                        context.lexicalPair().getDefaultContextSupport()
                ));
                continue;
            }
            for (LexicalPairExampleEntity example : orderedExamples) {
                candidates.add(new ExampleSelection(
                        example,
                        sense,
                        normalizeMeaning(sense.getChineseDefinition(), context.lexicalPair().getChineseGloss()),
                        firstNonBlank(example.getFrenchExample(), example.getEnglishExample()),
                        normalizeContextLevel(example.getContextSupportLevel(), context.lexicalPair().getDefaultContextSupport())
                ));
            }
        }

        if (candidates.isEmpty()) {
            return new ExampleSelection(
                    null,
                    null,
                    normalizeMeaning(null, context.lexicalPair().getChineseGloss()),
                    null,
                    context.lexicalPair().getDefaultContextSupport()
            );
        }

        long seed = context.sessionSeed() + context.exposureIndex() * 37L + context.lexicalPair().getId();
        Random random = new Random(seed);
        return candidates.get(random.nextInt(candidates.size()));
    }

    private List<TrainingOptionPayload> buildChoiceOptions(
            String correctMeaning,
            List<String> distractors,
            GenerationContext context,
            int size
    ) {
        List<String> labels = new ArrayList<>();
        labels.add(correctMeaning);
        for (String distractor : distractors) {
            if (labels.size() >= size) {
                break;
            }
            if (!labels.contains(distractor)) {
                labels.add(distractor);
            }
        }
        while (labels.size() < size) {
            labels.add("需要更多语境再判断");
        }

        long seed = context.sessionSeed() + context.exposureIndex() * 13L + context.lexicalPair().getId();
        Random random = new Random(seed);
        int correctIndex = random.nextInt(labels.size());
        if (correctIndex != 0) {
            String correct = labels.getFirst();
            labels.set(0, labels.get(correctIndex));
            labels.set(correctIndex, correct);
        }

        List<TrainingOptionPayload> options = new ArrayList<>();
        for (int i = 0; i < labels.size(); i++) {
            String key = i == correctIndex ? "correct" : "option_" + i;
            options.add(new TrainingOptionPayload(key, labels.get(i)));
        }
        return options;
    }

    private List<String> buildDistractors(LexicalPairEntity pair, String correctMeaning, LexicalPairSenseEntity correctSense) {
        Set<String> distractors = new LinkedHashSet<>(splitGlosses(pair.getChineseGloss()));
        if (correctSense != null) {
            distractors.removeIf(correctMeaning::equalsIgnoreCase);
        }
        if (LexicalPairType.fromCode(pair.getLexicalPairType()) == LexicalPairType.FALSE_FRIEND) {
            distractors.add("英语直觉含义");
            distractors.add("字面同形义");
        } else {
            distractors.add("仅凭字形做出的误判");
            distractors.add("需要额外辨析的相近义");
        }
        distractors.remove(correctMeaning);
        return distractors.stream().filter(Objects::nonNull).filter(value -> !value.isBlank()).toList();
    }

    private boolean isDirectMatchPair(LexicalPairEntity pair) {
        LexicalPairType pairType = LexicalPairType.fromCode(pair.getLexicalPairType());
        return pairType == LexicalPairType.COGNATE
                || (pairType == LexicalPairType.PARTIAL_COGNATE && pair.getSemanticOverlapScore().doubleValue() >= 0.60);
    }

    private boolean isStablePositiveTransfer(LexicalPairEntity pair) {
        LexicalPairType pairType = LexicalPairType.fromCode(pair.getLexicalPairType());
        return pairType == LexicalPairType.COGNATE
                || (pairType == LexicalPairType.PARTIAL_COGNATE && pair.getSemanticOverlapScore().doubleValue() >= 0.55);
    }

    private String normalizeMeaning(String preferred, String fallback) {
        return firstNonBlank(preferred, splitGlosses(fallback).stream().findFirst().orElse(fallback));
    }

    private List<String> splitGlosses(String glossary) {
        if (glossary == null || glossary.isBlank()) {
            return List.of("核心词义");
        }
        return Arrays.stream(glossary.split("[；;/,，]"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeContextLevel(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return ContextSupportLevel.fromCode(value).name();
    }

    private int contextWeight(String value, boolean preferHighContext) {
        ContextSupportLevel level = ContextSupportLevel.fromCode(firstNonBlank(value, ContextSupportLevel.MEDIUM.name()));
        return switch (level) {
            case HIGH -> preferHighContext ? 3 : 2;
            case MEDIUM -> 2;
            case LOW -> preferHighContext ? 1 : 3;
        };
    }

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback == null ? null : fallback.trim();
    }

    private String resolveCorrectAnswerKey(List<TrainingOptionPayload> options, String correctMeaning) {
        return options.stream()
                .filter(option -> option.label().equals(correctMeaning))
                .map(TrainingOptionPayload::key)
                .findFirst()
                .orElse(options.getFirst().key());
    }

    private record ExampleSelection(
            LexicalPairExampleEntity example,
            LexicalPairSenseEntity selectedSense,
            String correctMeaning,
            String contextSentence,
            String contextSupportLevel
    ) {
    }
}
