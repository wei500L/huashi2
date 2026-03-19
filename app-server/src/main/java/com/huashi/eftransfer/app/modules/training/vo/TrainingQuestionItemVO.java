package com.huashi.eftransfer.app.modules.training.vo;

import com.huashi.eftransfer.app.modules.training.support.TrainingStimulusPayload;

import java.util.List;

public record TrainingQuestionItemVO(
        Long itemResultId,
        Long planItemId,
        String mode,
        String itemType,
        Integer presentationOrder,
        Long lexicalPairId,
        String englishWord,
        String frenchWord,
        String chineseGloss,
        String lexicalPairType,
        TrainingWordPairVO wordPair,
        Integer difficultyLevel,
        String cognitiveTag,
        TrainingExerciseContentVO content,
        TrainingStimulusPayload stimulus,
        List<TrainingOptionViewVO> options
) {
}
