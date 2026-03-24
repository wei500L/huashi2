package com.huashi.eftransfer.app.common.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class PlatformEventOutboxService {

    private static final Logger log = LoggerFactory.getLogger(PlatformEventOutboxService.class);

    private final PlatformEventOutboxRepository repository;
    private final PlatformEventOutboxProperties properties;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public PlatformEventOutboxService(
            PlatformEventOutboxRepository repository,
            PlatformEventOutboxProperties properties,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.properties = properties;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public void enqueue(String eventId, String eventType, String exchangeName, String routingKey, Object payload, String traceId) {
        repository.insert(new PlatformOutboxMessage(
                eventId,
                eventType,
                exchangeName,
                routingKey,
                writeJson(payload),
                null,
                traceId
        ));
        log.info("event=platform_outbox_enqueued eventId={} eventType={} exchange={} routingKey={}",
                eventId, eventType, exchangeName, routingKey);
    }

    public int relayDueMessages() {
        List<PlatformEventOutboxRecord> claimed = repository.claimBatch(properties.getBatchSize(), properties.getStuckThreshold());
        for (PlatformEventOutboxRecord record : claimed) {
            relay(record);
        }
        return claimed.size();
    }

    public List<PlatformEventOutboxRecord> list(String status, int limit) {
        return repository.list(status, limit);
    }

    public PlatformEventOutboxRecord replay(Long id) {
        PlatformEventOutboxRecord existing = repository.findById(id);
        if (existing == null) {
            throw new IllegalStateException("Outbox record was not found: " + id);
        }
        if (existing.status() == PlatformEventOutboxStatus.IN_PROGRESS || existing.status() == PlatformEventOutboxStatus.PUBLISHED) {
            throw new IllegalStateException("Outbox record cannot be replayed from status: " + existing.status().name());
        }

        PlatformEventOutboxRecord record = repository.replay(id);
        if (record == null) {
            throw new IllegalStateException("Outbox record was not found: " + id);
        }
        relay(record);
        PlatformEventOutboxRecord refreshed = repository.findById(id);
        log.info("event=platform_outbox_replayed id={} eventId={} eventType={}", record.id(), record.eventId(), record.eventType());
        return refreshed == null ? record : refreshed;
    }

    private void relay(PlatformEventOutboxRecord record) {
        try {
            publish(record);
            repository.markPublished(record.id());
            log.info("event=platform_outbox_published id={} eventId={} eventType={} attemptCount={}",
                    record.id(), record.eventId(), record.eventType(), record.attemptCount() + 1);
        } catch (Exception ex) {
            OffsetDateTime nextAttemptAt = nextAttemptAt(record.attemptCount() + 1);
            repository.markFailed(record.id(), nextAttemptAt, ex.getMessage());
            log.warn("event=platform_outbox_publish_failed id={} eventId={} eventType={} attemptCount={} nextAttemptAt={} message={}",
                    record.id(), record.eventId(), record.eventType(), record.attemptCount() + 1, nextAttemptAt, ex.getMessage());
        }
    }

    private void publish(PlatformEventOutboxRecord record) throws Exception {
        CorrelationData correlationData = new CorrelationData(record.eventId());
        rabbitTemplate.send(record.exchangeName(), record.routingKey(), buildMessage(record), correlationData);
        CorrelationData.Confirm confirm = correlationData.getFuture().get(properties.getConfirmTimeout().toMillis(), TimeUnit.MILLISECONDS);
        if (confirm == null || !confirm.isAck()) {
            throw new IllegalStateException(confirm == null ? "Rabbit publisher confirm timed out" : confirm.getReason());
        }
        if (correlationData.getReturned() != null) {
            throw new IllegalStateException("Rabbit message was returned: " + correlationData.getReturned().getReplyText());
        }
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

    private OffsetDateTime nextAttemptAt(int attemptCount) {
        long initialMillis = properties.getInitialBackoff().toMillis();
        long maxMillis = properties.getMaxBackoff().toMillis();
        long multiplier = 1L << Math.min(Math.max(0, attemptCount - 1), 10);
        long delayMillis = Math.min(maxMillis, Math.max(initialMillis, initialMillis * multiplier));
        return OffsetDateTime.now(ZoneOffset.UTC).plus(Duration.ofMillis(delayMillis));
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize outbox payload", ex);
        }
    }
}
