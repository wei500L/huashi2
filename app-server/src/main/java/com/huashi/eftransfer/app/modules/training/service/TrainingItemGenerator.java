package com.huashi.eftransfer.app.modules.training.service;

import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairExampleEntity;
import com.huashi.eftransfer.app.modules.lexicon.entity.LexicalPairSenseEntity;
import com.huashi.eftransfer.app.modules.training.entity.TrainingPlanItemEntity;
import com.huashi.eftransfer.app.modules.training.support.TrainingOptionPayload;
import com.huashi.eftransfer.app.modules.training.support.TrainingStimulusPayload;
import com.huashi.eftransfer.shared.enums.TrainingCognitiveTag;
import com.huashi.eftransfer.shared.enums.TrainingItemType;

import java.util.List;

public interface TrainingItemGenerator {

    GeneratedItem generate(GenerationContext context);

    record SenseBundle(
            LexicalPairSenseEntity sense,
            List<LexicalPairExampleEntity> examples
    ) {
    }

    record GenerationContext(
            TrainingPlanItemEntity planItem,
            LexicalPairEntity lexicalPair,
            List<SenseBundle> senseBundles,
            int exposureIndex,
            long sessionSeed
    ) {
    }

    record GeneratedItem(
            TrainingItemType itemType,
            TrainingCognitiveTag cognitiveTag,
            TrainingStimulusPayload stimulus,
            List<TrainingOptionPayload> options,
            String correctAnswerKey
    ) {
    }
}
