package com.huashi.eftransfer.app.modules.opsconfig.support;

public record AiOpsConfigDiffEntry(
        String field,
        String before,
        String after
) {
}
