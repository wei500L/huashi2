package com.huashi.eftransfer.app.modules.training.support;

public record TrainingSessionLaunchContext(
        String launchSource,
        Long diagnosisSummaryId,
        Long lexicalPairId,
        Long wrongBookId,
        Long reviewScheduleId
) {
    public boolean targeted() {
        return lexicalPairId != null;
    }
}
