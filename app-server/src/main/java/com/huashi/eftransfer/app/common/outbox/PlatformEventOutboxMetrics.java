package com.huashi.eftransfer.app.common.outbox;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PlatformEventOutboxMetrics {

    public PlatformEventOutboxMetrics(MeterRegistry meterRegistry, PlatformEventOutboxRepository repository) {
        Gauge.builder("app.outbox.pending.count", repository, PlatformEventOutboxRepository::countPending)
                .description("Current pending outbox rows waiting to be relayed")
                .register(meterRegistry);
        Gauge.builder("app.outbox.failed.count", repository, PlatformEventOutboxRepository::countFailed)
                .description("Current failed outbox rows waiting for retry or replay")
                .register(meterRegistry);
        Gauge.builder("app.outbox.oldest.pending.age.seconds", repository, PlatformEventOutboxRepository::oldestPendingAgeSeconds)
                .description("Age in seconds of the oldest non-published outbox row")
                .register(meterRegistry);
    }
}
