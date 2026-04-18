package com.huashi.eftransfer.app.modules.opsconfig.support;

public final class AiRuntimeSyncOutboxSupport {

    public static final String EXCHANGE_NAME = "LOCAL";
    public static final String EVENT_TYPE = "AI_CONFIG_RUNTIME_SYNC_REQUESTED";
    public static final String ROUTING_KEY = "ai.runtime.sync.requested.v1";
    private static final String EVENT_ID_PREFIX = "ai-runtime-sync:";

    private AiRuntimeSyncOutboxSupport() {
    }

    public static String eventId(Long targetVersion) {
        return EVENT_ID_PREFIX + targetVersion;
    }
}
