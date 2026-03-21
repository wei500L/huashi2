package com.huashi.eftransfer.app.common.outbox;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "app.events.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class PlatformEventOutboxRelayScheduler {

    private final PlatformEventOutboxService outboxService;
    private final PlatformEventOutboxProperties properties;

    public PlatformEventOutboxRelayScheduler(
            PlatformEventOutboxService outboxService,
            PlatformEventOutboxProperties properties
    ) {
        this.outboxService = outboxService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "#{@platformEventOutboxProperties.pollInterval.toMillis()}")
    public void relay() {
        if (!properties.isEnabled()) {
            return;
        }
        outboxService.relayDueMessages();
    }
}
