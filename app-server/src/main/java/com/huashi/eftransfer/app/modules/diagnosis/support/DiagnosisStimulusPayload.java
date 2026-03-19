package com.huashi.eftransfer.app.modules.diagnosis.support;

public record DiagnosisStimulusPayload(
        String instruction,
        String contextSentence,
        String promptText
) {
}
