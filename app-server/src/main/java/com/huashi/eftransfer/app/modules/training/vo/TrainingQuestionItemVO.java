package com.huashi.eftransfer.app.modules.training.vo;

import com.huashi.eftransfer.app.modules.training.support.TrainingStimulusPayload;
import com.huashi.eftransfer.shared.enums.LexicalPairType;
import com.huashi.eftransfer.shared.enums.TrainingCognitiveTag;
import com.huashi.eftransfer.shared.enums.TrainingItemType;
import com.huashi.eftransfer.shared.enums.TrainingMode;

import java.util.List;

public record TrainingQuestionItemVO(
        Long itemResultId,
        Long planItemId,
        TrainingMode mode,
        TrainingItemType itemType,
        Integer presentationOrder,
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
        List<TrainingOptionViewVO> options
) {
}
