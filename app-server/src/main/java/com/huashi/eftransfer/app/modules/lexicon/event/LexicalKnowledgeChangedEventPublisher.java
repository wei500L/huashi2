package com.huashi.eftransfer.app.modules.lexicon.event;

import com.huashi.eftransfer.app.common.outbox.PlatformEventOutboxService;
import com.huashi.eftransfer.shared.event.LexicalKnowledgeChangedEvent;
import com.huashi.eftransfer.shared.event.PlatformEventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LexicalKnowledgeChangedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LexicalKnowledgeChangedEventPublisher.class);

    private final PlatformEventOutboxService outboxService;
    private final boolean enabled;

    public LexicalKnowledgeChangedEventPublisher(PlatformEventOutboxService outboxService, boolean enabled) {
        this.outboxService = outboxService;
        this.enabled = enabled;
    }

    public void publish(LexicalKnowledgeChangedEvent event) {
        if (!enabled) {
            log.warn("event=lexical_knowledge_changed_publish_disabled eventId={} sourceType={} sourceIds={}",
                    event.eventId(), event.sourceType(), event.sourceIds());
            return;
        }
        outboxService.enqueue(
                event.eventId(),
                LexicalKnowledgeChangedEvent.class.getSimpleName(),
                PlatformEventTopics.PLATFORM_EVENTS_EXCHANGE,
                PlatformEventTopics.LEXICAL_KNOWLEDGE_CHANGED_ROUTING_KEY,
                event,
                event.traceId()
        );
        log.info("event=lexical_knowledge_changed_event_enqueued eventId={} sourceType={} sourceIds={}",
                event.eventId(), event.sourceType(), event.sourceIds());
    }
}
