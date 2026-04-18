package com.huashi.eftransfer.app.modules.training.vo;

import com.huashi.eftransfer.app.modules.training.support.TrainingStimulusPayload;
import com.huashi.eftransfer.shared.enums.LexicalPairType;
import com.huashi.eftransfer.shared.enums.TrainingCognitiveTag;
import com.huashi.eftransfer.shared.enums.TrainingItemType;
import com.huashi.eftransfer.shared.enums.TrainingMode;

import java.time.LocalDateTime;
import java.util.List;

public record TrainingItemResultDetailVO(
        Long itemResultId,
        Long planItemId,
        Integer presentationOrder,
        TrainingMode mode,
        TrainingItemType itemType,
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        LexicalPairType lexicalPairType,
        TrainingWordPairVO wordPair,
        Integer difficultyLevel,
        TrainingCognitiveTag cognitiveTag,
        TrainingExerciseContentVO content,
        TrainingStimulusPayload stimulus,
        List<TrainingOptionViewVO> options,
        String correctAnswerKey,
        String selectedAnswerKey,
        LocalDateTime submittedAt,
        Integer reactionTimeMs,
        Integer hesitationTimeMs,
        Boolean correct,
        String detectedErrorType,
        Boolean reviewRequired,
        String adaptationAction
) {
}
