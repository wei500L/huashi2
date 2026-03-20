package com.huashi.eftransfer.shared.event;

public final class PlatformEventTopics {

    public static final String PLATFORM_EVENTS_EXCHANGE = "ef.transfer.platform.events";
    public static final String LEXICAL_KNOWLEDGE_CHANGED_ROUTING_KEY = "knowledge.lexical.changed.v1";
    public static final String AI_GATEWAY_KNOWLEDGE_SYNC_QUEUE = "ai-gateway.knowledge-sync.queue";
    public static final String AI_GATEWAY_KNOWLEDGE_SYNC_DLQ = "ai-gateway.knowledge-sync.dlq";

    private PlatformEventTopics() {
    }
}
