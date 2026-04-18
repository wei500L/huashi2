package com.huashi.eftransfer.app.common.config;

import com.huashi.eftransfer.app.common.outbox.PlatformEventOutboxService;
import com.huashi.eftransfer.app.modules.lexicon.event.LexicalKnowledgeChangedEventPublisher;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;

@Configuration
public class RabbitMqEventConfig {

    @Bean
    public TopicExchange platformEventsExchange() {
        return new TopicExchange(com.huashi.eftransfer.shared.event.PlatformEventTopics.PLATFORM_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public LexicalKnowledgeChangedEventPublisher lexicalKnowledgeChangedEventPublisher(
            RabbitTemplate rabbitTemplate,
            PlatformEventOutboxService outboxService,
            @Value("${app.events.rabbit-publish-enabled:true}") boolean rabbitPublishEnabled
    ) {
        rabbitTemplate.setMandatory(true);
        return new LexicalKnowledgeChangedEventPublisher(outboxService, rabbitPublishEnabled);
    }
}
