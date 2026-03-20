package com.huashi.eftransfer.app.modules.lexicon.event;

import com.huashi.eftransfer.shared.event.LexicalKnowledgeChangedEvent;
import com.huashi.eftransfer.shared.event.PlatformEventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class LexicalKnowledgeChangedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LexicalKnowledgeChangedEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final boolean enabled;

    public LexicalKnowledgeChangedEventPublisher(RabbitTemplate rabbitTemplate, boolean enabled) {
        this.rabbitTemplate = rabbitTemplate;
        this.enabled = enabled;
    }

    public void publish(LexicalKnowledgeChangedEvent event) {
        if (!enabled) {
            return;
        }
        rabbitTemplate.convertAndSend(
                PlatformEventTopics.PLATFORM_EVENTS_EXCHANGE,
                PlatformEventTopics.LEXICAL_KNOWLEDGE_CHANGED_ROUTING_KEY,
                event
        );
        log.info("event=lexical_knowledge_changed_event_published eventId={} sourceType={} sourceIds={}",
                event.eventId(), event.sourceType(), event.sourceIds());
    }
}
