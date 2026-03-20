package com.huashi.eftransfer.ai.modules.rag.config;

import com.huashi.eftransfer.shared.event.PlatformEventTopics;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@EnableRabbit
public class KnowledgeSyncRabbitConfig {

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange platformEventsExchange() {
        return new TopicExchange(PlatformEventTopics.PLATFORM_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue knowledgeSyncQueue() {
        return new Queue(
                PlatformEventTopics.AI_GATEWAY_KNOWLEDGE_SYNC_QUEUE,
                true,
                false,
                false,
                Map.of(
                        "x-dead-letter-exchange", PlatformEventTopics.PLATFORM_EVENTS_EXCHANGE,
                        "x-dead-letter-routing-key", PlatformEventTopics.AI_GATEWAY_KNOWLEDGE_SYNC_DLQ
                )
        );
    }

    @Bean
    public Queue knowledgeSyncDlq() {
        return new Queue(PlatformEventTopics.AI_GATEWAY_KNOWLEDGE_SYNC_DLQ, true);
    }

    @Bean
    public Binding knowledgeSyncBinding(Queue knowledgeSyncQueue, TopicExchange platformEventsExchange) {
        return BindingBuilder.bind(knowledgeSyncQueue)
                .to(platformEventsExchange)
                .with(PlatformEventTopics.LEXICAL_KNOWLEDGE_CHANGED_ROUTING_KEY);
    }

    @Bean
    public Binding knowledgeSyncDlqBinding(Queue knowledgeSyncDlq, TopicExchange platformEventsExchange) {
        return BindingBuilder.bind(knowledgeSyncDlq)
                .to(platformEventsExchange)
                .with(PlatformEventTopics.AI_GATEWAY_KNOWLEDGE_SYNC_DLQ);
    }
}
