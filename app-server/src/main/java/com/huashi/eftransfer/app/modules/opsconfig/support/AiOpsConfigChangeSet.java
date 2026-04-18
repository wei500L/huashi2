package com.huashi.eftransfer.app.modules.opsconfig.support;

import java.util.List;

public record AiOpsConfigChangeSet(
        List<AiOpsConfigDiffEntry> configDiffs,
        List<AiOpsSecretChangeEntry> secretChanges
) {
    public boolean hasChanges() {
        return !configDiffs.isEmpty() || !secretChanges.isEmpty();
    }
}
