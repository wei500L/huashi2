package com.huashi.eftransfer.app.common.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class PlatformEventOutboxService {

    private static final Logger log = LoggerFactory.getLogger(PlatformEventOutboxService.class);

    private final PlatformEventOutboxRepository repository;
    private final PlatformEventOutboxProperties properties;
    private final List<PlatformEventOutboxRelayHandler> relayHandlers;
    private final ObjectMapper objectMapper;

    public PlatformEventOutboxService(
            PlatformEventOutboxRepository repository,
            PlatformEventOutboxProperties properties,
            List<PlatformEventOutboxRelayHandler> relayHandlers,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.properties = properties;
        this.relayHandlers = relayHandlers == null ? List.of() : List.copyOf(relayHandlers);
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
            relay(record, false);
        }
        return claimed.size();
    }

    public List<PlatformEventOutboxRecord> list(String status, int limit) {
        if (status == null || status.isBlank()) {
            List<PlatformEventOutboxRecord> merged = new ArrayList<>(repository.list(null, limit));
            merged.addAll(repository.listDlq(limit));
            merged.sort(Comparator
                    .comparing(PlatformEventOutboxRecord::updatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(PlatformEventOutboxRecord::id, Comparator.nullsLast(Comparator.reverseOrder())));
            return merged.stream().limit(limit).toList();
        }
        if (PlatformEventOutboxStatus.DLQ.name().equalsIgnoreCase(status.trim())) {
            return repository.listDlq(limit);
        }
        return repository.list(status, limit);
    }

    public PlatformEventOutboxRecord findByEventId(String eventId) {
        PlatformEventOutboxRecord active = repository.findByEventId(eventId);
        return active != null ? active : repository.findDlqByEventId(eventId);
    }

    public PlatformEventOutboxRecord replay(Long id) {
        PlatformEventOutboxRecord existing = findAnyById(id);
        if (existing == null) {
            throw new IllegalStateException("Outbox record was not found: " + id);
        }
        if (existing.status() == PlatformEventOutboxStatus.IN_PROGRESS || existing.status() == PlatformEventOutboxStatus.PUBLISHED) {
            throw new IllegalStateException("Outbox record cannot be replayed from status: " + existing.status().name());
        }

        boolean replayedFromDlq = existing.status() == PlatformEventOutboxStatus.DLQ;
        PlatformEventOutboxRecord record = replayedFromDlq
                ? repository.restoreDlqToPending(id)
                : repository.replay(id);
        if (record == null) {
            throw new IllegalStateException("Outbox record was not found: " + id);
        }
        relay(record, replayedFromDlq);
        PlatformEventOutboxRecord refreshed = findAnyById(id);
        log.info("event=platform_outbox_replayed id={} eventId={} eventType={}", record.id(), record.eventId(), record.eventType());
        return refreshed == null ? record : refreshed;
    }

    private void relay(PlatformEventOutboxRecord record, boolean replayedFromDlq) {
        PlatformEventOutboxRelayHandler handler = findHandler(record);
        if (handler == null) {
            int nextAttemptCount = record.attemptCount() + 1;
            repository.moveToDlq(record.id(), nextAttemptCount, "No outbox relay handler for eventType " + record.eventType());
            log.warn("event=platform_outbox_moved_to_dlq id={} eventId={} eventType={} attemptCount={} reason=no_handler",
                    record.id(), record.eventId(), record.eventType(), nextAttemptCount);
            return;
        }
        try {
            String detail = handler.relay(record);
            repository.markPublished(record.id());
            boolean recoveredFromFailure = replayedFromDlq || record.attemptCount() > 0;
            handler.afterPublished(record, recoveredFromFailure, detail);
            log.info("event=platform_outbox_published id={} eventId={} eventType={} attemptCount={} detail={}",
                    record.id(), record.eventId(), record.eventType(), record.attemptCount() + 1, detail);
        } catch (Exception ex) {
            int nextAttemptCount = record.attemptCount() + 1;
            if (!handler.isRetryableFailure(ex) || nextAttemptCount >= properties.getMaxAttempts()) {
                repository.moveToDlq(record.id(), nextAttemptCount, ex.getMessage());
                handler.afterMovedToDlq(record, nextAttemptCount, ex);
                log.warn("event=platform_outbox_moved_to_dlq id={} eventId={} eventType={} attemptCount={} message={}",
                        record.id(), record.eventId(), record.eventType(), nextAttemptCount, ex.getMessage());
                return;
            }

            OffsetDateTime nextAttemptAt = nextAttemptAt(nextAttemptCount);
            repository.markFailed(record.id(), nextAttemptAt, ex.getMessage());
            handler.afterRetryScheduled(record, nextAttemptCount, nextAttemptAt, ex);
            log.warn("event=platform_outbox_publish_failed id={} eventId={} eventType={} attemptCount={} nextAttemptAt={} message={}",
                    record.id(), record.eventId(), record.eventType(), nextAttemptCount, nextAttemptAt, ex.getMessage());
        }
    }

    private PlatformEventOutboxRecord findAnyById(Long id) {
        PlatformEventOutboxRecord active = repository.findById(id);
        if (active != null) {
            return active;
        }
        return repository.findDlqById(id);
    }

    private PlatformEventOutboxRelayHandler findHandler(PlatformEventOutboxRecord record) {
        for (PlatformEventOutboxRelayHandler handler : relayHandlers) {
            if (handler.supports(record)) {
                return handler;
            }
        }
        return null;
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
