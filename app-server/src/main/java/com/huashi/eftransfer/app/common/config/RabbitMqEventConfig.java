package com.huashi.eftransfer.app.common.config;

import com.huashi.eftransfer.app.modules.diagnosis.event.DiagnosisCompletedEventPublisher;
import com.huashi.eftransfer.app.modules.diagnosis.event.RabbitDiagnosisCompletedEventPublisher;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqEventConfig {

    public static final String DIAGNOSIS_EVENTS_EXCHANGE = "ef.transfer.events";
    public static final String DIAGNOSIS_COMPLETED_ROUTING_KEY = "diagnosis.completed.v1";
    public static final String DIAGNOSIS_ANALYTICS_QUEUE = "diagnosis.analytics.queue";

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public TopicExchange diagnosisEventsExchange() {
        return new TopicExchange(DIAGNOSIS_EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue diagnosisAnalyticsQueue() {
        return new Queue(DIAGNOSIS_ANALYTICS_QUEUE, true);
    }

    @Bean
    public Binding diagnosisAnalyticsBinding(Queue diagnosisAnalyticsQueue, TopicExchange diagnosisEventsExchange) {
        return BindingBuilder.bind(diagnosisAnalyticsQueue)
                .to(diagnosisEventsExchange)
                .with(DIAGNOSIS_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public DiagnosisCompletedEventPublisher diagnosisCompletedEventPublisher(RabbitTemplate rabbitTemplate) {
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
        return new RabbitDiagnosisCompletedEventPublisher(rabbitTemplate);
    }
}
