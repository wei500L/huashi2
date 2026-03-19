package com.huashi.eftransfer.app.modules.diagnosis.event;

import com.huashi.eftransfer.app.common.config.RabbitMqEventConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class RabbitDiagnosisCompletedEventPublisher implements DiagnosisCompletedEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitDiagnosisCompletedEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitDiagnosisCompletedEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publish(DiagnosisCompletedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMqEventConfig.DIAGNOSIS_EVENTS_EXCHANGE,
                RabbitMqEventConfig.DIAGNOSIS_COMPLETED_ROUTING_KEY,
                event
        );
        log.info("event=diagnosis_completed_event_published sessionId={} summaryId={} templateId={} ownerUserId={}",
                event.sessionId(), event.summaryId(), event.templateId(), event.ownerUserId());
    }
}
