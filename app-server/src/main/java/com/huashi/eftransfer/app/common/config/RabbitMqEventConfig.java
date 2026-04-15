package com.huashi.eftransfer.app.common.config;

import com.huashi.eftransfer.app.common.outbox.PlatformEventOutboxService;
import com.huashi.eftransfer.app.modules.diagnosis.event.DiagnosisCompletedEventPublisher;
import com.huashi.eftransfer.app.modules.lexicon.event.LexicalKnowledgeChangedEventPublisher;
import com.huashi.eftransfer.app.modules.training.event.ApplicationTrainingCompletedEventPublisher;
import com.huashi.eftransfer.app.modules.training.event.TrainingCompletedEventPublisher;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqEventConfig {

    @Bean
    public TopicExchange platformEventsExchange() {
        return new TopicExchange(com.huashi.eftransfer.shared.event.PlatformEventTopics.PLATFORM_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public DiagnosisCompletedEventPublisher diagnosisCompletedEventPublisher(
            ApplicationEventPublisher applicationEventPublisher
    ) {
        return applicationEventPublisher::publishEvent;
    }

    @Bean
    public TrainingCompletedEventPublisher trainingCompletedEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new ApplicationTrainingCompletedEventPublisher(applicationEventPublisher);
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
