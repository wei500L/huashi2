package com.huashi.eftransfer.app.common.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
@Order(100)
public class RabbitPlatformEventOutboxRelayHandler implements PlatformEventOutboxRelayHandler {

    private static final Logger log = LoggerFactory.getLogger(RabbitPlatformEventOutboxRelayHandler.class);

    private final RabbitTemplate rabbitTemplate;
    private final PlatformEventOutboxProperties properties;

    public RabbitPlatformEventOutboxRelayHandler(
            RabbitTemplate rabbitTemplate,
            PlatformEventOutboxProperties properties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public boolean supports(PlatformEventOutboxRecord record) {
        return record != null;
    }

    @Override
    public String relay(PlatformEventOutboxRecord record) throws Exception {
        CorrelationData correlationData = new CorrelationData(record.eventId());
        rabbitTemplate.send(record.exchangeName(), record.routingKey(), buildMessage(record), correlationData);
        CorrelationData.Confirm confirm = correlationData.getFuture().get(properties.getConfirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
        if (confirm == null || !confirm.isAck()) {
            throw new IllegalStateException(confirm == null ? "Rabbit publisher confirm timed out" : confirm.getReason());
        }
        if (correlationData.getReturned() != null) {
            throw new IllegalStateException("Rabbit message was returned: " + correlationData.getReturned().getReplyText());
        }
        return "published to rabbit";
    }

    @Override
    public void afterPublished(PlatformEventOutboxRecord record, boolean recoveredFromFailure, String detail) {
        log.info("event=platform_outbox_rabbit_published id={} eventId={} eventType={} detail={}",
                record.id(), record.eventId(), record.eventType(), detail);
    }

    private Message buildMessage(PlatformEventOutboxRecord record) {
        var builder = MessageBuilder.withBody(record.payloadJson().getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(record.eventId())
                .setHeader("eventType", record.eventType());
        if (record.traceId() != null && !record.traceId().isBlank()) {
            builder.setHeader("traceId", record.traceId());
        }
        return builder.build();
    }
}
