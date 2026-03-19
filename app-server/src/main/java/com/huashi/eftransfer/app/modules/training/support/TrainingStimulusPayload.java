package com.huashi.eftransfer.app.modules.training.support;

public record TrainingStimulusPayload(
        String instruction,
        String questionText,
        String contextSentence,
        String explanation,
        String contextSupportLevel
) {
}
